/*! Vision Widget — Medium 달력 (실시간 요일·날짜·월 달력, 오늘 하이라이트)
   - data-live="true" : 현재 날짜로 갱신 (자정 넘어가면 자동 갱신)
   - URL ?date=2027-12-20 또는 data-date : 특정 날짜로 미리보기/QA */
(function(){
  var W=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],
      M=['January','February','March','April','May','June','July','August','September','October','November','December'];
  function parse(s){var m=/^(\d{4})-(\d{2})-(\d{2})$/.exec(s||'');return m?new Date(+m[1],m[2]-1,+m[3]):null}
  var qs=parse((location.search.match(/[?&]date=([\d-]+)/)||[])[1]);
  function render(){
    document.querySelectorAll('.widget[data-live="true"]').forEach(function(w){
      var d=parse(w.dataset.date)||qs||new Date(), key=d.toDateString();
      if(w.dataset.rendered===key)return; w.dataset.rendered=key;
      w.querySelectorAll('[data-wd]').forEach(function(e){e.textContent=W[d.getDay()]});
      w.querySelectorAll('[data-d]').forEach(function(e){e.textContent=d.getDate()});
      w.querySelectorAll('[data-mon]').forEach(function(e){e.textContent=M[d.getMonth()]});
      var first=new Date(d.getFullYear(),d.getMonth(),1).getDay(), days=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
      var h='SMTWTFS'.split('').map(function(c){return '<span class="dow">'+c+'</span>'}).join('');
      for(var i=0;i<first;i++)h+='<span></span>';
      for(var k=1;k<=days;k++)h+=(k===d.getDate()?'<span class="today">':'<span>')+k+'</span>';
      w.querySelector('.grid').innerHTML=h;
      w.dataset.weeks=Math.ceil((first+days)/7);
    });
  }
  render(); setInterval(render,30000);
})();
