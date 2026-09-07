const params = new URLSearchParams(location.search);
const party = params.get('party');
const token = params.get('token');
let agreement;

const $ = s => document.querySelector(s);

function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, m => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'
  })[m]);
}

function show(t, c) {
  $('#notice').textContent = t;
  $('#notice').className = 'notice ' + (c || '');
}

function d(k, v) {
  return `<div class="detail"><span>${esc(k)}</span><strong>${esc(v)}</strong></div>`;
}

function renderDetails() {
  $('#details').innerHTML = `
    ${d('Agreement Number', agreement.agreementNumber)}
    ${d('Client', agreement.candidateFullName)}
    ${d('Execution Date', agreement.executionDate)}
    ${d('Total Fee', agreement.totalFeeAmount)}
    ${d('Pre-Payment', agreement.prePaymentAmount)}
    ${d('Balance', agreement.balanceAmount)}
    ${d('Agreement Status', agreement.status)}
    ${d('Client Signature', agreement.candidateSigningStatus)}
    ${d('Consultant Signature', agreement.consultantSigningStatus)}
    ${d('Agreement Valid', agreement.valid ? 'YES - Fully Executed' : 'NO - Both Signatures Required')}
  `;
}

function updateSigningStatus() {
  if (!agreement) return;

  const already = party === 'candidate'
    ? agreement.candidateSigningStatus === 'SIGNED'
    : agreement.consultantSigningStatus === 'SIGNED';

  const btn = $('#signBtn');

  if (agreement.status === 'FULLY_SIGNED') {
    btn.disabled = true;
    btn.textContent = '✓ Agreement Fully Executed';
    $('#signStatus').innerHTML = '<div class="success-status">✓ Both parties have signed. This agreement is fully executed and valid.</div>';
    return;
  }

  if (already) {
    btn.disabled = true;
    btn.textContent = '✓ Already Signed';
    $('#signStatus').innerHTML = '<div class="success-status">✓ Your signature has already been recorded. The agreement is not fully valid until both parties have signed.</div>';
    return;
  }

  if (party === 'consultant' && agreement.candidateSigningStatus !== 'SIGNED') {
    btn.disabled = true;
    btn.textContent = 'Waiting for Client Signature';
    $('#signStatus').innerHTML = '<div class="waiting-status">Client review and signature is required before consultant signing can proceed.</div>';
    return;
  }

  btn.disabled = false;
  btn.textContent = '🔐 Continue with Aadhaar OTP eSign';
}

async function init() {
  if (!party || !token || !['candidate','consultant'].includes(party)) {
    show('Invalid signing link.', 'error');
    return;
  }

  try {
    const r = await fetch(`/api/sign/${encodeURIComponent(party)}/${encodeURIComponent(token)}`);
    if (!r.ok) {
      show('This signing link is invalid or expired.', 'error');
      return;
    }

    agreement = await r.json();
    $('#heading').textContent = agreement.agreementNumber;
    $('#partyName').textContent = party === 'candidate' ? agreement.candidateFullName : 'PV Talent Partners';
    $('#partyText').textContent = party === 'candidate'
      ? 'You are signing as the Client.'
      : 'You are signing as the Consultant / Proprietor.';

    renderDetails();
    $('#pdfViewer').src = `/api/sign/${encodeURIComponent(party)}/${encodeURIComponent(token)}/pdf`;
    updateSigningStatus();
  } catch (e) {
    console.error(e);
    show('Unable to load the agreement.', 'error');
  }
}

async function startSign() {
  if (!agreement) {
    show('Agreement is not loaded.', 'error');
    return;
  }

  if (!$('#consent').checked) {
    show('Please review the agreement and provide your consent before signing.', 'error');
    return;
  }

  const button = $('#signBtn');
  button.disabled = true;
  button.textContent = 'Starting secure eSign…';

  try {
    const response = await fetch(
      `/api/agreements/${agreement.id}/esign/start?party=${encodeURIComponent(party)}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Signing-Token': token
        },
        body: JSON.stringify({
          consent: true,
          idProofType: $('#idProofType').value || null,
          idProofNumber: $('#idProofNumber').value.trim() || null
        })
      }
    );

    const data = await response.json();

    if (!response.ok) throw new Error(data.message || 'Unable to start eSign.');

    if (data.signingUrl) {
      window.location.href = data.signingUrl;
      return;
    }

    if (data.transactionId) {
      show('eSign transaction created. Connect the authorised provider redirect to continue with Aadhaar OTP.', 'success');
      button.disabled = false;
      button.textContent = '🔐 Continue with Aadhaar OTP eSign';
      return;
    }

    throw new Error('No eSign URL returned by the server.');
  } catch (e) {
    console.error(e);
    show(e.message || 'Unable to start Aadhaar OTP eSign.', 'error');
    button.disabled = false;
    button.textContent = '🔐 Continue with Aadhaar OTP eSign';
  }
}

async function demoSign() {
  if (!confirm('Demo only: mark this party as signed?')) return;

  try {
    const r = await fetch(`/api/agreements/${agreement.id}/demo-sign?party=${encodeURIComponent(party)}`, {
      method:'POST',
      headers: {'X-Signing-Token': token}
    });
    if (!r.ok) {
      show('Demo signing is disabled.', 'error');
      return;
    }

    agreement = await r.json();
    renderDetails();
    updateSigningStatus();
    show('Signature recorded in the local prototype.', 'success');
  } catch (e) {
    console.error(e);
    show('Demo signing failed.', 'error');
  }
}

init();
