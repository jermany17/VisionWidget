/*! Vision Widget C2 — 실시간 요일·날짜·월 달력(오늘 하이라이트)
   - data-live="true"  : 현재 날짜로 갱신 (자정 넘어가면 자동 갱신)
   - data-date="2027-12-20" 또는 페이지 URL ?date=2027-12-20 : 특정 날짜로 미리보기/QA                     */
(function(){
  var LONG=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
  function parse(s){var m=/^(\d{4})-(\d{2})-(\d{2})$/.exec(s||'');return m?new Date(+m[1],m[2]-1,+m[3]):null}
  var qs=parse((location.search.match(/[?&]date=([\d-]+)/)||[])[1]);
  function render(){
    document.querySelectorAll('.vw2[data-live="true"]').forEach(function(w){
      var d=parse(w.dataset.date)||qs||new Date();
      var key=d.getFullYear()+'-'+d.getMonth()+'-'+d.getDate();
      if(w.dataset.rendered===key)return; w.dataset.rendered=key;
      var name=LONG[d.getDay()]; if(w.dataset.dayfmt==='short')name=name.slice(0,3);
      w.querySelector('.dname').textContent=name;
      w.querySelector('.dnum').textContent=d.getDate();
      w.querySelector('.month').textContent=d.getMonth()+1;
      var first=new Date(d.getFullYear(),d.getMonth(),1).getDay(), days=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
      var h='SMTWTFS'.split('').map(function(c){return '<span class="dow">'+c+'</span>'}).join('');
      for(var i=0;i<first;i++)h+='<span></span>';
      for(var k=1;k<=days;k++)h+=(k===d.getDate()?'<span class="today">':'<span>')+k+'</span>';
      w.querySelector('.grid').innerHTML=h;
      w.dataset.weeks=Math.ceil((first+days)/7);
      fit(w);
    });
  }
  /* 요일명이 날짜 블록보다 길면(예: WEDNESDAY) 글자 크기를 비율로 축소 — cqw 기반이라 위젯 크기와 무관 */
  function fit(w){
    var dn=w.querySelector('.dname'),box=w.querySelector('.day'); if(!dn||!box)return;
    dn.style.removeProperty('--fit');
    var avail=box.clientWidth, need=dn.scrollWidth;
    if(need>avail) dn.style.setProperty('--fit',(avail/need*0.97).toFixed(3));
  }
  function fitAll(){document.querySelectorAll('.vw2').forEach(fit)}
  render(); setInterval(render,30000);
  if(document.fonts&&document.fonts.ready)document.fonts.ready.then(fitAll);
})();
