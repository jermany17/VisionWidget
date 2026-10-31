/*! Vision Widget — Coffee : 시계/날짜 실시간 갱신. ?time=HH:MM · ?date=YYYY-MM-DD 로 미리보기 */
(function(){
  var DOW=['SUN','MON','TUE','WED','THU','FRI','SAT'], LONG=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
  var qt=location.search.match(/[?&]time=(\d{1,2}):(\d{2})/), qd=location.search.match(/[?&]date=(\d{4})-(\d{2})-(\d{2})/);
  function now(){var d=new Date(); if(qd){d.setFullYear(+qd[1],qd[2]-1,+qd[3]);} if(qt){d.setHours(+qt[1],+qt[2],0,0);} return d;}
  function set(w,sel,v){var e=w.querySelector(sel); if(e&&e.textContent!==v)e.textContent=v;}
  function render(){
    var d=now(), h=d.getHours(), m=d.getMinutes();
    document.querySelectorAll('.cw[data-live="true"]').forEach(function(w){
      var hh=w.querySelector('.hand.hh'), mm=w.querySelector('.hand.mm');
      if(hh){ hh.style.transform='rotate('+((h%12)*30+m*0.5)+'deg)'; mm.style.transform='rotate('+(m*6)+'deg)'; }
      set(w,'.time',(h%12||12)+':'+(m<10?'0':'')+m);
      set(w,'.dow-day',DOW[d.getDay()]+' '+d.getDate());
      set(w,'.dow',DOW[d.getDay()]);
      set(w,'.day',String(d.getDate()));
      set(w,'.wd-long',LONG[d.getDay()]);
      set(w,'.dow-mon',DOW[d.getDay()]+' · '+['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'][d.getMonth()]);
    });
  }
  render(); setInterval(render,10000);
})();
