/*! Vision Widget — widgets.js : data-live="true" 위젯의 시간/날짜/캘린더 갱신 */
(function(){
  var MON=['January','February','March','April','May','June','July','August','September','October','November','December'];
  var DOW=['SUN','MON','TUE','WED','THU','FRI','SAT'];
  function pad(n){return (n<10?'0':'')+n}
  function render(){
    var now=new Date();
    document.querySelectorAll('.widget[data-live="true"]').forEach(function(w){
      var k=w.dataset.kind;
      if(k==='date'){
        w.querySelector('.month').textContent=MON[now.getMonth()].slice(0,3).toUpperCase();
        w.querySelector('.day').textContent=now.getDate();
        w.querySelector('.weekday').textContent=DOW[now.getDay()];
      }else if(k==='time'){
        var h=now.getHours(),ap=h<12?'AM':'PM';h=h%12||12;
        w.querySelector('.time').textContent=h+':'+pad(now.getMinutes());
        w.querySelector('.ampm').textContent=ap;
      }else if(k==='calendar'){
        var key=now.getFullYear()+'-'+now.getMonth()+'-'+now.getDate();
        if(w.dataset.rendered===key)return;
        w.dataset.rendered=key;
        w.querySelector('.cal-title').textContent=MON[now.getMonth()];
        var html='SMTWTFS'.split('').map(function(c){return '<span class="dow">'+c+'</span>'}).join('');
        var first=new Date(now.getFullYear(),now.getMonth(),1).getDay();
        var days=new Date(now.getFullYear(),now.getMonth()+1,0).getDate();
        for(var i=0;i<first;i++)html+='<span></span>';
        for(var d=1;d<=days;d++)html+=(d===now.getDate()?'<span class="today">':'<span>')+d+'</span>';
        w.querySelector('.cal-grid').innerHTML=html;
      }
    });
  }
  render();setInterval(render,10000);
})();
