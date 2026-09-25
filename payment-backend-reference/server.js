require('dotenv').config();
const express = require('express');
const crypto = require('crypto');
const admin = require('firebase-admin');
const Razorpay = require('razorpay');

function requireFirebaseDatabaseRootUrl() {
  const value = String(process.env.FIREBASE_DATABASE_URL || '').trim();
  if (!value) {
    throw new Error('FIREBASE_DATABASE_URL is required. Use the Realtime Database root URL only.');
  }
  const parsed = new URL(value);
  if (parsed.protocol !== 'https:' || (parsed.pathname && parsed.pathname !== '/')) {
    throw new Error('FIREBASE_DATABASE_URL must be an HTTPS Realtime Database root URL with no child path.');
  }
  return value.replace(/\/$/, '');
}

function firebaseCredential() {
  // Best Render option: store the complete service-account JSON in one secret env var.
  const json = String(process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '').trim();
  if (json) {
    let serviceAccount;
    try {
      serviceAccount = JSON.parse(json);
    } catch (e) {
      throw new Error('FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON.');
    }
    return admin.credential.cert(serviceAccount);
  }

  // Alternative Render option: provide the three service-account fields separately.
  const projectId = String(process.env.FIREBASE_PROJECT_ID || '').trim();
  const clientEmail = String(process.env.FIREBASE_CLIENT_EMAIL || '').trim();
  const privateKeyRaw = String(process.env.FIREBASE_PRIVATE_KEY || '');
  if (projectId || clientEmail || privateKeyRaw) {
    if (!projectId || !clientEmail || !privateKeyRaw) {
      throw new Error('Set FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL and FIREBASE_PRIVATE_KEY together.');
    }
    return admin.credential.cert({
      projectId,
      clientEmail,
      privateKey: privateKeyRaw.replace(/\\n/g, '\n')
    });
  }

  // Local/cloud fallback: GOOGLE_APPLICATION_CREDENTIALS or provider workload identity.
  return admin.credential.applicationDefault();
}

admin.initializeApp({
  credential: firebaseCredential(),
  databaseURL: requireFirebaseDatabaseRootUrl()
});
const db = admin.database();
const razorpayEnabled = Boolean(
  process.env.RAZORPAY_KEY_ID && process.env.RAZORPAY_KEY_SECRET &&
  !process.env.RAZORPAY_KEY_ID.includes('xxxxxxxx') &&
  !process.env.RAZORPAY_KEY_SECRET.includes('xxxxxxxx')
);
const razorpay = razorpayEnabled
  ? new Razorpay({ key_id: process.env.RAZORPAY_KEY_ID, key_secret: process.env.RAZORPAY_KEY_SECRET })
  : null;
const app = express();

function hmac(payload, secret) {
  return crypto.createHmac('sha256', secret).update(payload).digest('hex');
}

function currentMonthKeyIndia() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit'
  }).formatToParts(new Date());
  const year = parts.find(p => p.type === 'year').value;
  const month = parts.find(p => p.type === 'month').value;
  return `${year}-${month}`;
}

// Webhook must receive raw bytes so the signature can be verified.
app.post('/payments/webhook', express.raw({ type: 'application/json' }), async (req, res) => {
  try {
    if (!razorpayEnabled || !process.env.RAZORPAY_WEBHOOK_SECRET) return res.status(503).send('payment gateway disabled');
    const received = req.get('x-razorpay-signature') || '';
    const expected = hmac(req.body, process.env.RAZORPAY_WEBHOOK_SECRET);
    if (received.length !== expected.length || !crypto.timingSafeEqual(Buffer.from(received), Buffer.from(expected))) {
      return res.status(401).send('invalid signature');
    }

    const event = JSON.parse(req.body.toString('utf8'));
    if (event.event === 'payment.captured') {
      const payment = event.payload.payment.entity;
      const orderSnap = await db.ref(`paymentOrders/${payment.order_id}`).get();
      if (orderSnap.exists()) {
        const order = orderSnap.val();
        await recordVerifiedPayment(order, payment.id, payment.order_id, 'captured_webhook');
      }
    }
    res.sendStatus(200);
  } catch (e) {
    console.error(e);
    res.sendStatus(400);
  }
});

app.get('/health', (req, res) => {
  res.json({ ok: true, payouts: true, payments: razorpayEnabled });
});

app.use(express.json({ limit: '64kb' }));

async function auth(req, res, next) {
  try {
    const value = req.get('authorization') || '';
    if (!value.startsWith('Bearer ')) return res.status(401).json({ error: 'missing auth token' });
    req.user = await admin.auth().verifyIdToken(value.slice(7));
    next();
  } catch (e) {
    res.status(401).json({ error: 'invalid auth token' });
  }
}

app.post('/payments/create-order', auth, async (req, res) => {
  try {
    if (!razorpayEnabled) return res.status(503).json({ error: 'online payment gateway is disabled; offline payments and payout finalisation still work' });
    const { groupId } = req.body;
    if (!groupId) return res.status(400).json({ error: 'groupId required' });
    const monthKey = currentMonthKeyIndia();

    // Never trust the amount sent by the phone. Load the authoritative amount from Firebase.
    const groupSnap = await db.ref(`groups/${groupId}`).get();
    if (!groupSnap.exists()) return res.status(404).json({ error: 'group not found' });
    const group = groupSnap.val();
    if (!group.members || !group.members[req.user.uid]) return res.status(403).json({ error: 'not a group member' });

    const existingPayment = await db.ref(`payments/${groupId}/${monthKey}/${req.user.uid}`).get();
    if (existingPayment.exists() && existingPayment.child('verified').val() === true) {
      return res.status(409).json({ error: 'this month is already paid and verified' });
    }

    const amountPaise = Math.round(Number(group.monthlyAmount) * 100);
    if (!Number.isFinite(amountPaise) || amountPaise <= 0) return res.status(400).json({ error: 'invalid contribution amount' });

    const order = await razorpay.orders.create({
      amount: amountPaise,
      currency: 'INR',
      receipt: `bhishi_${groupId.slice(-8)}_${monthKey}_${req.user.uid.slice(0, 6)}`,
      notes: { groupId, monthKey, uid: req.user.uid }
    });

    await db.ref(`paymentOrders/${order.id}`).set({
      uid: req.user.uid,
      groupId,
      monthKey,
      amount: amountPaise,
      currency: 'INR',
      status: 'created',
      createdAt: admin.database.ServerValue.TIMESTAMP
    });

    res.json({ orderId: order.id, keyId: process.env.RAZORPAY_KEY_ID, amount: order.amount, currency: order.currency });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'could not create order' });
  }
});

app.post('/payments/verify', auth, async (req, res) => {
  try {
    if (!razorpayEnabled) return res.status(503).json({ verified: false, error: 'online payment gateway is disabled' });
    const { razorpay_payment_id, razorpay_order_id, razorpay_signature } = req.body;
    if (!razorpay_payment_id || !razorpay_order_id || !razorpay_signature) return res.status(400).json({ error: 'missing payment fields' });

    // Retrieve the original order from the server. Do not trust group/amount/order metadata from the phone.
    const orderSnap = await db.ref(`paymentOrders/${razorpay_order_id}`).get();
    if (!orderSnap.exists()) return res.status(404).json({ error: 'unknown order' });
    const order = orderSnap.val();
    if (order.uid !== req.user.uid) return res.status(403).json({ error: 'order belongs to another user' });

    const expected = hmac(`${razorpay_order_id}|${razorpay_payment_id}`, process.env.RAZORPAY_KEY_SECRET);
    const validSignature = expected.length === razorpay_signature.length &&
      crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(razorpay_signature));
    if (!validSignature) return res.status(400).json({ verified: false, error: 'signature mismatch' });

    const payment = await razorpay.payments.fetch(razorpay_payment_id);
    if (payment.order_id !== razorpay_order_id || Number(payment.amount) !== Number(order.amount) || payment.currency !== 'INR') {
      return res.status(400).json({ verified: false, error: 'payment details mismatch' });
    }
    if (!['authorized', 'captured'].includes(payment.status)) {
      return res.status(400).json({ verified: false, error: `payment status ${payment.status}` });
    }

    // Signature verification proves authenticity, but the Bhishi ledger is credited only
    // after Razorpay reports the payment as captured. If it is merely authorised, the
    // payment.captured webhook below will reconcile it without trusting the phone.
    if (payment.status !== 'captured') {
      await db.ref(`paymentOrders/${razorpay_order_id}`).update({
        status: 'authorized_waiting_capture',
        paymentId: razorpay_payment_id
      });
      return res.status(202).json({ verified: true, captured: false, paymentId: razorpay_payment_id, status: payment.status });
    }

    await recordVerifiedPayment(order, razorpay_payment_id, razorpay_order_id, payment.status);
    res.json({ verified: true, captured: true, paymentId: razorpay_payment_id, status: payment.status });
  } catch (e) {
    console.error(e);
    res.status(500).json({ verified: false, error: 'verification failed' });
  }
});

async function recordVerifiedPayment(order, paymentId, orderId, gatewayStatus) {
  const amountRupees = Number(order.amount) / 100;
  const memberSnap = await db.ref(`groups/${order.groupId}/members/${order.uid}`).get();
  const memberName = memberSnap.exists() && memberSnap.child('name').val()
    ? memberSnap.child('name').val() : 'Member';
  const updates = {};
  updates[`payments/${order.groupId}/${order.monthKey}/${order.uid}`] = {
    uid: order.uid,
    groupId: order.groupId,
    monthKey: order.monthKey,
    amount: amountRupees,
    currency: 'INR',
    gateway: 'razorpay',
    orderId,
    paymentId,
    gatewayStatus,
    verified: true,
    verifiedAt: admin.database.ServerValue.TIMESTAMP
  };
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/uid`] = order.uid;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/memberName`] = memberName;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/amount`] = amountRupees;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/onTime`] = true;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/timestamp`] = admin.database.ServerValue.TIMESTAMP;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/confirmedByMember`] = true;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/paid`] = true;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/paymentVerified`] = true;
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/paymentMethod`] = 'gateway';
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/paymentStatus`] = 'verified';
  updates[`contributions/${order.groupId}/${order.monthKey}/${order.uid}/paymentId`] = paymentId;
  updates[`paymentOrders/${orderId}/status`] = 'verified';
  updates[`paymentOrders/${orderId}/paymentId`] = paymentId;
  await db.ref().update(updates);
}



// Server-authoritative Bhishi payout finalisation. The phone can request a close,
// but it cannot choose or overwrite the winner.
app.post('/payouts/finalize', auth, async (req, res) => {
  try {
    const { groupId } = req.body;
    if (!groupId) return res.status(400).json({ error: 'groupId required' });
    const monthKey = currentMonthKeyIndia();

    const [groupSnap, contributionSnap, previousPayoutsSnap] = await Promise.all([
      db.ref(`groups/${groupId}`).get(),
      db.ref(`contributions/${groupId}/${monthKey}`).get(),
      db.ref(`payouts/${groupId}`).get()
    ]);

    if (!groupSnap.exists()) return res.status(404).json({ error: 'group not found' });
    const group = groupSnap.val();
    if (group.adminId !== req.user.uid) return res.status(403).json({ error: 'admin access required' });

    const members = Object.values(group.members || {}).filter(m => m && m.uid);
    if (members.length < 2) return res.status(400).json({ error: 'at least two members are required' });

    const contributions = contributionSnap.val() || {};
    const unverified = members.filter(member => {
      const c = contributions[member.uid];
      return !c || c.confirmedByMember !== true || c.confirmedByAdmin !== true;
    });
    if (unverified.length) {
      return res.status(409).json({ error: `${unverified.length} contribution(s) are not dual-verified` });
    }

    const priorWinners = new Set();
    previousPayoutsSnap.forEach(child => {
      const payout = child.val();
      if (payout && payout.lockedResult === true && payout.winnerUid) priorWinners.add(payout.winnerUid);
    });

    const eligible = members.filter(member => !priorWinners.has(member.uid));
    if (!eligible.length) return res.status(409).json({ error: 'no members remain eligible for a payout' });

    const monthlyAmount = Number(group.monthlyAmount);
    const poolAmount = monthlyAmount * members.length;
    if (!Number.isFinite(poolAmount) || poolAmount <= 0) return res.status(400).json({ error: 'invalid group amount' });

    // A cryptographic nonce is generated server-side and written once. The stored audit hash
    // lets the group reproduce the draw/tie-break material after the result is locked.
    const seedRef = db.ref(`roundSeeds/${groupId}/${monthKey}`);
    let seedSnap = await seedRef.get();
    let nonce;
    if (seedSnap.exists()) {
      const value = seedSnap.val();
      nonce = typeof value === 'object' && value.nonce ? String(value.nonce) : String(value);
    } else {
      nonce = crypto.randomBytes(24).toString('hex');
      const seedTxn = await seedRef.transaction(current => current || {
        nonce,
        createdAt: admin.database.ServerValue.TIMESTAMP
      });
      const stored = seedTxn.snapshot.val();
      nonce = typeof stored === 'object' && stored.nonce ? String(stored.nonce) : String(stored);
    }

    eligible.sort((a, b) => String(a.uid).localeCompare(String(b.uid)));
    let payout;
    const mode = String(group.mode || 'draw').toLowerCase();

    if (mode === 'auction') {
      // Close the visible bidding window on the trusted server before reading bids.
      // This prevents a last-millisecond client bid from being accepted after the server
      // has already taken its bid snapshot. The admin can reopen only if finalisation fails.
      const roundConfigRef = db.ref(`roundConfigs/${groupId}/${monthKey}`);
      const roundConfigSnap = await roundConfigRef.get();
      if (!roundConfigSnap.exists()) {
        return res.status(409).json({ error: 'open the sealed auction before finalising it' });
      }
      const roundConfig = roundConfigSnap.val() || {};
      if (!roundConfig.openAt || !roundConfig.closeAt) {
        return res.status(409).json({ error: 'auction window is invalid' });
      }
      await roundConfigRef.update({
        status: 'closed',
        closeAt: Math.min(Number(roundConfig.closeAt) || Date.now(), Date.now()),
        closedAt: admin.database.ServerValue.TIMESTAMP
      });

      const bidSnap = await db.ref(`auctionBids/${groupId}/${monthKey}`).get();
      const eligibleIds = new Set(eligible.map(m => m.uid));
      const bids = [];
      bidSnap.forEach(child => {
        const bid = child.val();
        const uid = child.key; // identity comes from the authenticated user's bid path, not client payload
        const amount = Number(bid && bid.amount);
        if (bid && eligibleIds.has(uid) && Number.isFinite(amount) && amount > 0 && amount <= poolAmount) {
          bids.push({ uid, memberName: bid.memberName || 'Member', amount });
        }
      });
      if (!bids.length) return res.status(409).json({ error: 'no eligible sealed bids have been submitted' });

      const lowest = Math.min(...bids.map(b => b.amount));
      const tied = bids.filter(b => Math.abs(b.amount - lowest) < 0.001)
        .sort((a, b) => String(a.uid).localeCompare(String(b.uid)));
      const material = `${groupId}|${monthKey}|${nonce}|${lowest}|${tied.map(b => b.uid).join('|')}`;
      const digest = crypto.createHash('sha256').update(material).digest('hex');
      const index = Number(BigInt(`0x${digest}`) % BigInt(tied.length));
      const winningBid = tied[index];
      const winningMember = eligible.find(m => m.uid === winningBid.uid) || winningBid;
      const dividend = members.length > 1 ? Math.max(0, poolAmount - lowest) / (members.length - 1) : 0;

      payout = {
        monthKey,
        winnerUid: winningBid.uid,
        winnerName: winningMember.name || winningBid.memberName || 'Member',
        mode: 'auction',
        bidAmount: lowest,
        dividendPerMember: dividend,
        poolAmount,
        eligibleCount: bids.length,
        auditNonce: nonce,
        auditHash: digest.slice(0, 16).toUpperCase(),
        lockedResult: true,
        timestamp: Date.now()
      };
    } else {
      const material = `${groupId}|${monthKey}|${nonce}|${eligible.map(m => m.uid).join('|')}`;
      const digest = crypto.createHash('sha256').update(material).digest('hex');
      const index = Number(BigInt(`0x${digest}`) % BigInt(eligible.length));
      const winner = eligible[index];

      payout = {
        monthKey,
        winnerUid: winner.uid,
        winnerName: winner.name || 'Member',
        mode: 'draw',
        bidAmount: 0,
        dividendPerMember: 0,
        poolAmount,
        eligibleCount: eligible.length,
        auditNonce: nonce,
        auditHash: digest.slice(0, 16).toUpperCase(),
        lockedResult: true,
        timestamp: Date.now()
      };
    }

    // Transaction makes the result write-once even if the admin double-taps or two requests race.
    const payoutRef = db.ref(`payouts/${groupId}/${monthKey}`);
    const payoutTxn = await payoutRef.transaction(current => current && current.lockedResult === true ? undefined : payout);
    if (!payoutTxn.committed) {
      const existing = payoutTxn.snapshot.val();
      return res.status(409).json({ error: 'this payout round is already locked', payout: existing || null });
    }

    await db.ref(`groups/${groupId}/members/${payout.winnerUid}/hasReceivedPayout`).set(true);
    if (mode === 'auction') {
      await db.ref(`roundConfigs/${groupId}/${monthKey}`).update({
        status: 'closed',
        finalizedAt: admin.database.ServerValue.TIMESTAMP
      });
    }
    return res.json({ finalized: true, payout });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'could not finalise payout round' });
  }
});

const port = Number(process.env.PORT || 8080);
app.listen(port, '0.0.0.0', () => console.log(`Bhishi payment backend listening on ${port}`));
