// ── Distributor App — License Server (Google Apps Script) ─────────────────
// Deploy: Extensions → Apps Script → Deploy → New Deployment
//         Type: Web app | Execute as: Me | Who has access: Anyone
// ─────────────────────────────────────────────────────────────────────────────

const SHEET_NAME = 'Lisensi';

function doPost(e) {
  try {
    const data   = JSON.parse(e.postData.contents);
    const action = data.action;
    if (action === 'activate') return handleActivate(data);
    if (action === 'check')    return handleCheck(data);
    return respond({ success: false, error: 'UNKNOWN_ACTION' });
  } catch (err) {
    return respond({ success: false, error: err.message });
  }
}

// ── Aktivasi pertama kali ──────────────────────────────────────────────────
function handleActivate(data) {
  const { key, device_id, device_model, app_version } = data;
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const sheet = getSheet();
    const rows  = sheet.getDataRange().getValues();

    for (let i = 1; i < rows.length; i++) {
      const row = rows[i];
      if (row[0] !== key) continue;

      const status   = row[2];  // C
      const deviceId = row[6];  // G

      if (status === 'REVOKED')  return respond({ success: false, error: 'REVOKED' });
      if (status === 'EXPIRED')  return respond({ success: false, error: 'EXPIRED' });

      // Kunci sudah pernah diaktifkan di device lain
      if ((status === 'TRIAL' || status === 'ACTIVE') && deviceId && deviceId !== device_id) {
        return respond({ success: false, error: 'ALREADY_ACTIVATED_OTHER_DEVICE' });
      }

      // PENDING atau device yang sama (install ulang) → lanjutkan
      const trialDays  = row[3] || 30;
      const now        = new Date();
      const expiry     = new Date(now);
      expiry.setDate(expiry.getDate() + Number(trialDays));

      const r = i + 1;
      sheet.getRange(r, 3).setValue('TRIAL');
      sheet.getRange(r, 5).setValue(now.toISOString());
      sheet.getRange(r, 6).setValue(expiry.toISOString());
      sheet.getRange(r, 7).setValue(device_id);
      sheet.getRange(r, 8).setValue(device_model  || '');
      sheet.getRange(r, 9).setValue(app_version   || '');
      sheet.getRange(r, 10).setValue(now.toISOString());

      const daysLeft = Math.max(0, Math.ceil((expiry - now) / 86400000));
      return respond({
        success: true, status: 'TRIAL',
        days_remaining: daysLeft,
        expiry_date: expiry.toISOString()
      });
    }

    return respond({ success: false, error: 'KEY_NOT_FOUND' });
  } finally {
    lock.releaseLock();
  }
}

// ── Pengecekan rutin saat app dibuka ───────────────────────────────────────
function handleCheck(data) {
  const { key, device_id, app_version } = data;
  const lock = LockService.getScriptLock();
  lock.waitLock(5000);
  try {
    const sheet = getSheet();
    const rows  = sheet.getDataRange().getValues();

    for (let i = 1; i < rows.length; i++) {
      const row      = rows[i];
      if (row[0] !== key) continue;

      const status   = row[2];
      const expiry   = row[5];
      const deviceId = row[6];

      if (status === 'REVOKED') return respond({ success: false, error: 'REVOKED' });
      if (deviceId && deviceId !== device_id) return respond({ success: false, error: 'DEVICE_MISMATCH' });

      const r   = i + 1;
      const now = new Date();
      sheet.getRange(r, 10).setValue(now.toISOString());
      if (app_version) sheet.getRange(r, 9).setValue(app_version);

      if (status === 'ACTIVE') {
        return respond({ success: true, status: 'ACTIVE', days_remaining: -1 });
      }

      if (status === 'TRIAL' || status === 'PENDING') {
        if (!expiry) return respond({ success: false, error: 'NOT_ACTIVATED' });
        const expiryDate = new Date(expiry);
        if (now > expiryDate) {
          sheet.getRange(r, 3).setValue('EXPIRED');
          return respond({ success: false, error: 'EXPIRED' });
        }
        const daysLeft = Math.max(0, Math.ceil((expiryDate - now) / 86400000));
        return respond({
          success: true, status: 'TRIAL',
          days_remaining: daysLeft,
          expiry_date: expiry
        });
      }

      return respond({ success: false, error: 'EXPIRED' });
    }

    return respond({ success: false, error: 'KEY_NOT_FOUND' });
  } finally {
    lock.releaseLock();
  }
}

// ── Helper ─────────────────────────────────────────────────────────────────
function respond(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

function getSheet() {
  return SpreadsheetApp.getActiveSpreadsheet().getSheetByName(SHEET_NAME);
}

// ── Generate kunci lisensi (jalankan manual dari Apps Script editor) ────────
// Contoh: generateKeys(5, 30, "Distributor Bandung")
//   count     = jumlah kunci
//   trialDays = durasi trial (hari)
//   label     = nama/keterangan (untuk referensi Anda)
function generateKeys(count, trialDays, label) {
  const sheet = getSheet();
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // tanpa 0,O,1,I (mudah terbaca)

  for (let k = 0; k < count; k++) {
    const p1  = randomPart(chars, 4);
    const p2  = randomPart(chars, 4);
    const key = `DIST-${p1}-${p2}`;
    sheet.appendRow([key, label || '', 'PENDING', trialDays || 30, '', '', '', '', '', '', '']);
  }
  SpreadsheetApp.getUi().alert(`${count} kunci berhasil dibuat.`);
}

function randomPart(chars, len) {
  return Array.from({ length: len }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}
