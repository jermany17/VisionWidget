/*! Vision Widget — c2-09 Blush glow : 요일·월·날짜·월 달력(오늘 표시) 실시간 갱신. ?date=YYYY-MM-DD 로 미리보기 */
(function(){
  var L=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
  function parse(s){var m=/^(\d{4})-(\d{2})-(\d{2})$/.exec(s||'');return m?new Date(+m[1],m[2]-1,+m[3]):null}
  var qs=parse((location.search.match(/[?&]date=([\d-]+)/)||[])[1]);
  function render(){
    document.querySelectorAll('.widget[data-live="true"]').forEach(function(w){
      var d=parse(w.dataset.date)||qs||new Date(),key=d.toDateString();
      if(w.dataset.rendered===key)return; w.dataset.rendered=key;
      w.querySelector('.weekday').textContent=L[d.getDay()];
      w.querySelector('.month').textContent=d.getMonth()+1;
      w.querySelector('.day-number').textContent=d.getDate();
      var first=new Date(d.getFullYear(),d.getMonth(),1).getDay(),days=new Date(d.getFullYear(),d.getMonth()+1,0).getDate(),h='';
      for(var i=0;i<first;i++)h+='<span></span>';
      for(var k=1;k<=days;k++)h+=(k===d.getDate()?'<span class="selected">':'<span>')+k+'</span>';
      w.querySelector('.days').innerHTML=h;
      w.dataset.weeks=Math.ceil((first+days)/7);
    });
  }
  render(); setInterval(render,30000);
})();
