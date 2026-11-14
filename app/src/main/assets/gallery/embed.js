(function () {
  // Every design's own root is renamed to this on the way in, whatever the design
  // called it, so there is one name to look for rather than a list to keep current.
  var ROOTS = '.widget';

  function fit() {
    var root = document.querySelector(ROOTS);
    if (!root) return;

    // Measured with nothing of ours applied, so this is the design's own size.
    root.style.transform = 'none';
    var rect = root.getBoundingClientRect();
    var frame = document.documentElement.clientWidth;
    if (!rect.width || !frame) return;

    var scale = frame / rect.width;
    root.style.transformOrigin = '0 0';
    root.style.transform =
      'scale(' + scale + ') translate(' + (-rect.left) + 'px,' + (-rect.top) + 'px)';

    // A transform moves what is drawn, not the box it was drawn in, so the page is
    // still the size the design laid itself out at. Scaled up, the design then runs
    // past the bottom of that box and the page clips it away — which is why the clocks
    // lost their lower half and the calendars their last column. Telling the page how
    // big the design now is puts the edge back where it belongs.
    document.body.style.width = (rect.width * scale) + 'px';
    document.body.style.height = (rect.height * scale) + 'px';
  }

  fit();
  // Measured again once everything is settled. The first run happens as soon as the
  // document is parsed, which can be before the page's own viewport width has been
  // applied — and a design laid out wider than it was drawn then gets the wrong figure
  // and runs off the bottom of its frame.
  requestAnimationFrame(fit);
  window.addEventListener('load', fit);
  // The shelf hands a tile its size after the page is already up, and the same page is
  // shown again larger elsewhere.
  window.addEventListener('resize', fit);
  // Type arrives late and can change what the design measures out to.
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(fit);
})();