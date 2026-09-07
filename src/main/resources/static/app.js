const $=s=>document.querySelector(s);
const api=path=>fetch(path);

function openCreate(){ $('#modal').classList.remove('hidden'); }
function closeCreate(){ $('#modal').classList.add('hidden'); }

async function loadAgreements(){
  const res=await api('/api/agreements');
  if(res.status===401){ location.href='/login.html'; return; }
  const data=await res.json();
  $('#total').textContent=data.length;
  $('#awaiting').textContent=data.filter(x=>x.status==='AWAITING_CANDIDATE').length;
  $('#consultAwaiting').textContent=data.filter(x=>x.status==='AWAITING_CONSULTANT').length;
  $('#complete').textContent=data.filter(x=>x.status==='FULLY_SIGNED').length;
  $('#rows').innerHTML=data.map(a=>`
    <tr>
      <td><b>${esc(a.agreementNumber)}</b></td>
      <td>${esc(a.candidateFullName)}</td>
      <td>${esc(a.totalFeeAmount)}</td>
      <td>${badge(a.candidateSigningStatus)}</td>
      <td>${badge(a.consultantSigningStatus)}</td>
      <td class="status">${a.valid ? '<span class="badge ok">✓ VALID</span>' : '<span class="badge pending">Not Valid Yet</span>'}</td>
      <td class="actions-cell">
        <button class="ghost" type="button" onclick="showSigningLinks('${a.id}')">Links</button>
        ${a.originalPdfPath ? `<a class="ghost" href="/api/agreements/${a.id}/pdf" target="_blank">PDF</a>` : '<span class="badge pending">PDF Required</span>'}
        <button class="danger" type="button" onclick="deleteForConsultant('${a.id}','${esc(a.agreementNumber)}')">Delete</button>
      </td>
    </tr>`).join('');
}
function badge(v){return `<span class="badge ${v==='SIGNED'?'ok':'pending'}">${v==='SIGNED'?'✓ Signed':'Pending'}</span>`}
function esc(v){return String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m]))}

async function showSigningLinks(id){
  try {
    const res=await api('/api/agreements/'+id);
    if(!res.ok) throw new Error('Unable to load agreement');
    const a=await res.json();
    const base=location.origin;
    const c=base+'/sign.html?party=candidate&token='+a.candidateSigningToken;
    const s=base+'/sign.html?party=consultant&token='+a.consultantSigningToken;
    alert('Client signing link:\n'+c+'\n\nConsultant signing link:\n'+s);
  } catch(e) {
    alert('Could not load signing links. Please refresh and try again.');
    console.error(e);
  }
}

async function deleteForConsultant(id, agreementNumber){
  const ok=confirm('Remove '+agreementNumber+' from the Consultant dashboard only?\n\nThe agreement will remain stored and the client/consultant signing links will continue to work.');
  if(!ok) return;
  try {
    const res=await fetch('/api/agreements/'+id+'/consultant',{method:'DELETE'});
    const body=await res.json().catch(()=>({}));
    if(!res.ok) throw new Error(body.message || 'Could not remove agreement');
    await loadAgreements();
    alert('Agreement removed from the Consultant dashboard.');
  } catch(e) { alert(e.message || 'Could not remove agreement'); console.error(e); }
}

$('#createForm').addEventListener('submit',async e=>{
  e.preventDefault();
  const form=e.target;
  const file=$('#agreementPdf').files[0];
  if(!file){ alert('Please select the final agreement PDF.'); return; }
  if(!file.name.toLowerCase().endsWith('.pdf')){ alert('Please upload a PDF. Convert DOC/DOCX to PDF before sending.'); return; }

  const obj=Object.fromEntries(new FormData(form).entries());
  delete obj.agreementPdf;

  const button=form.querySelector('button[type="submit"]');
  button.disabled=true;
  button.textContent='Creating agreement…';
  try {
    const res=await fetch('/api/agreements',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(obj)});
    if(!res.ok){ const error=await res.json().catch(()=>({})); throw new Error(error.message || 'Could not create agreement'); }
    const a=await res.json();

    button.textContent='Uploading PDF…';
    const fd=new FormData();
    fd.append('file',file);
    const uploadRes=await fetch('/api/agreements/'+a.id+'/pdf',{method:'POST',body:fd});
    if(!uploadRes.ok){ const error=await uploadRes.json().catch(()=>({})); throw new Error(error.message || 'PDF upload failed'); }

    button.textContent='Sending client email…';
    const sendRes=await fetch('/api/agreements/'+a.id+'/send',{method:'POST'});
    if(!sendRes.ok){ const error=await sendRes.json().catch(()=>({})); throw new Error(error.message || 'Client email could not be sent'); }

    closeCreate();
    form.reset();
    await loadAgreements();
    const base=location.origin;
    const c=base+'/sign.html?party=candidate&token='+a.candidateSigningToken;
    alert('Agreement created, PDF uploaded and client email sent.\n\nClient signing link:\n'+c+'\n\nThe client can now download/view the PDF, review it and complete Aadhaar OTP eSign.\n\nThe consultant will be notified only after the client signs.');
  } catch(e) {
    alert(e.message || 'Could not create the agreement.');
    console.error(e);
  } finally {
    button.disabled=false;
    button.textContent='Create, Upload & Email';
  }
});
loadAgreements();
