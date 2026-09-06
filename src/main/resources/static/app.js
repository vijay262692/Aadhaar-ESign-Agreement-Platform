const $=s=>document.querySelector(s);
const api=path=>fetch(path);

function openCreate(){ $('#modal').classList.remove('hidden'); }
function closeCreate(){ $('#modal').classList.add('hidden'); }

async function loadAgreements(){
  const res=await api('/api/agreements');
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
      <td class="status">${esc(a.status)}</td>
      <td class="actions-cell">
        <button class="ghost" onclick="links('${a.id}')">Links</button>
        <a class="ghost" href="/api/agreements/${a.id}/pdf" target="_blank">PDF</a>
      </td>
    </tr>`).join('');
}
function badge(v){return `<span class="badge ${v==='SIGNED'?'ok':'pending'}">${v==='SIGNED'?'✓ Signed':'Pending'}</span>`}
function esc(v){return String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m]))}

async function links(id){
  const a=await (await api('/api/agreements/'+id)).json();
  const base=location.origin;
  const c=base+'/sign.html?party=candidate&token='+a.candidateSigningToken;
  const s=base+'/sign.html?party=consultant&token='+a.consultantSigningToken;
  alert('Candidate signing link:\\n'+c+'\\n\\nConsultant signing link:\\n'+s);
}
$('#createForm').addEventListener('submit',async e=>{
  e.preventDefault();
  const obj=Object.fromEntries(new FormData(e.target).entries());
  const res=await fetch('/api/agreements',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(obj)});
  if(!res.ok){alert('Could not create agreement');return}
  const a=await res.json();
  closeCreate();e.target.reset();await loadAgreements();
  const base=location.origin;
  const c=base+'/sign.html?party=candidate&token='+a.candidateSigningToken;
  alert('Agreement created.\\n\\nCandidate signing link:\\n'+c);
});
loadAgreements();
