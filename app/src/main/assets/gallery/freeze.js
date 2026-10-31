(function () {
  var date = location.search.match(/[?&]date=(\d{4})-(\d{2})-(\d{2})/);
  var time = location.search.match(/[?&]time=(\d{1,2}):(\d{2})/);
  if (!date && !time) return;

  var Real = Date;
  var at = new Real();
  if (date) at.setFullYear(+date[1], date[2] - 1, +date[3]);
  if (time) at.setHours(+time[1], +time[2], 0, 0);
  var fixed = at.getTime();

  function Frozen() {
    if (arguments.length === 0) return new Real(fixed);
    var args = [null].concat(Array.prototype.slice.call(arguments));
    return new (Function.prototype.bind.apply(Real, args))();
  }
  Frozen.prototype = Real.prototype;
  Frozen.now = function () { return fixed; };
  Frozen.parse = Real.parse;
  Frozen.UTC = Real.UTC;

  window.Date = Frozen;
})();