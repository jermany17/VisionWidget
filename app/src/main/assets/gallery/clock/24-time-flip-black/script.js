/*! Vision Widget — Classic Flip : 시(12시간제, 앞자리 0 없음)·분 실시간 갱신. ?time=HH:MM 으로 미리보기 */
(function(){
  var q=(location.search.match(/[?&]time=(\d{1,2}):(\d{2})/)||null);
  function pad(n){return (n<10?'0':'')+n}
  function render(){
    var d=new Date(); if(q){d.setHours(+q[1]);d.setMinutes(+q[2]);}
    var h=String(d.getHours()%12||12), m=pad(d.getMinutes());
    document.querySelectorAll('.flip[data-live="true"]').forEach(function(w){
      [['.hh',h],['.mm',m]].forEach(function(p){var c=w.querySelector(p[0]),s=c.querySelector('span');
        if(s.textContent!==p[1]){s.textContent=p[1];c.classList.remove('tick');void c.offsetWidth;c.classList.add('tick');}});
    });
  }
  render(); setInterval(render,10000);
})();
