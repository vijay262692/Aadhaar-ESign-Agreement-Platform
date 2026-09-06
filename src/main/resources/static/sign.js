const params=new URLSearchParams(location.search);
const party=params.get('party');
const token=params.get('token');
let agreement;

const $=s=>document.querySelector(s);
function esc(v){return String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m])}

async function init(){
  if(!party||!token){show('Invalid signing link','error');return}
  const r=await fetch(`/api/sign/${encodeURIComponent(party)}/${encodeURIComponent(token)}`);
  if(!r.ok){show('This signing link is invalid or expired.','error');return}
  agreement=await r.json();
  $('#heading').textContent=agreement.agreementNumber;
  $('#partyName').textContent=party==='candidate'?agreement.candidateFullName:'PV Talent Partners';
  $('#partyText').textContent=party==='candidate'
    ? 'You are signing as the Candidate.'
    : 'You are signing as the Consultant / Proprietor.';
  $('#details').innerHTML=`
    ${d('Candidate',agreement.candidateFullName)}
    ${d('Execution Date',agreement.executionDate)}
    ${d('Total Fee',agreement.totalFeeAmount)}
    ${d('Pre-Payment',agreement.prePaymentAmount)}
    ${d('Balance',agreement.balanceAmount)}
    ${d('Agreement Status',agreement.status)}
    ${d('Candidate Signature',agreement.candidateSigningStatus)}
    ${d('Consultant Signature',agreement.consultantSigningStatus)}
  `;
  const already=party==='candidate'?agreement.candidateSigningStatus==='SIGNED':agreement.consultantSigningStatus==='SIGNED';
  if(already){$('#signBtn').disabled=true;$('#signBtn').textContent='✓ Already Signed';}
}
function d(k,v){return `<div class="detail"><span>${esc(k)}</span><strong>${esc(v)}</strong></div>`}
function show(t,c){$('#notice').textContent=t;$('#notice').className='notice '+c}
async function startSign(){
  const r=await fetch(`/api/agreements/${agreement.id}/esign/start?party=${party}`,{method:'POST'});
  const data=await r.json();
  // Replace this line with the real provider redirect URL returned by your authorised eSign integration.
  alert('eSign transaction created:\\n'+data.transactionId+'\\n\\nConnect your authorised eSign provider here for the actual Aadhaar OTP/signing flow.');
}
async function demoSign(){
  if(!confirm('Demo only: mark this party as signed?')) return;
  const r=await fetch(`/api/agreements/${agreement.id}/demo-sign?party=${party}`,{method:'POST'});
  if(!r.ok){show('Demo signing is disabled.','error');return}
  agreement=await r.json();
  show('Signature recorded in the local prototype.','success');
  $('#signBtn').textContent='✓ Signed';
  $('#signBtn').disabled=true;
  $('#details').innerHTML=`
    ${d('Candidate',agreement.candidateFullName)}
    ${d('Execution Date',agreement.executionDate)}
    ${d('Total Fee',agreement.totalFeeAmount)}
    ${d('Pre-Payment',agreement.prePaymentAmount)}
    ${d('Balance',agreement.balanceAmount)}
    ${d('Agreement Status',agreement.status)}
    ${d('Candidate Signature',agreement.candidateSigningStatus)}
    ${d('Consultant Signature',agreement.consultantSigningStatus)}
  `;
}
init();
