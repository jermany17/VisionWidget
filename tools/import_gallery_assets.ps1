# Brings the designed widgets into the app as assets.
#
# The source tree keeps one folder per widget, each with its own copy of whatever fonts
# it uses -- 205 files where there are only 18 distinct ones. Fonts are pooled into a
# shared folder here and the stylesheets are pointed at it, which is most of the weight
# gone. The untouched `원본_*.png` source art is left behind; only the .webp a design
# actually draws is carried over.
#
# Lock screen designs are skipped: there is no lock screen widget in this app.

param(
    [string]$Source = "C:\Users\leejm\Downloads\vision-widgets-by-widget-20261031T055010Z-1-001\vision-widgets-by-widget",
    [string]$Dest   = "C:\Users\leejm\AndroidStudioProjects\VisionWidget\app\src\main\assets\gallery"
)

$ErrorActionPreference = "Stop"
$Categories = @("clock", "calendar")

if (Test-Path $Dest) { Remove-Item $Dest -Recurse -Force }
New-Item -ItemType Directory -Path $Dest -Force | Out-Null
$fontsDir = Join-Path $Dest "_fonts"
New-Item -ItemType Directory -Path $fontsDir -Force | Out-Null

# ---- orders ------------------------------------------------------------------
function Read-Json($path) { Get-Content $path -Encoding UTF8 -Raw | ConvertFrom-Json }

$allOrder = Read-Json (Join-Path $Source "_order-all.json")
$allById = @{}
foreach ($i in $allOrder.items) { $allById[$i.id] = $i }

$catById = @{}
foreach ($c in $Categories) {
    $o = Read-Json (Join-Path $Source "$c\_order.json")
    foreach ($i in $o.items) { $catById[$i.id] = $i }
}

# ---- ratio parsing -----------------------------------------------------------
# "238:222", "1:1", "600:280", "480:255 (≈1.88:1)" -> width / height.
# The flip designs carry an object of three sizes; the small one is what the shelf shows.
function Get-Ratio($designRatio) {
    $text = if ($designRatio -is [string]) { $designRatio } else { [string]$designRatio.small }
    if ($text -match '(\d+(?:\.\d+)?)\s*:\s*(\d+(?:\.\d+)?)') {
        return [math]::Round([double]$Matches[1] / [double]$Matches[2], 6)
    }
    return 1.0
}

function Get-Cells($cells) {
    if ($cells -is [string]) { return $cells }
    return [string]$cells.small
}

function Get-Size($size) {
    $text = [string]$size
    if ($text -match '^\s*(\w+)') { return $Matches[1].ToLower() }
    return "small"
}

# The width a design was drawn at, in CSS pixels.
#
# Most of these size their root fluidly and measure everything inside in `cqw`, so they
# would fit any width given to them. Two of the clocks are laid out in fixed pixels
# instead, and those only hold together at the width they were drawn for. Handing the
# viewport that width covers both: the fluid ones fill it, the fixed ones land on it
# exactly, and the view scales the finished page to whatever size it is being shown at.
function Get-DesignWidth($cssPath) {
    if (-not (Test-Path $cssPath)) { return 340 }
    $css = (Get-Content $cssPath -Encoding UTF8 -Raw) -replace "`r?`n", " "

    $rule = [regex]::Match($css, '\.(widget|cw|vw2|flip|clock-widget)\s*\{[^}]*?width\s*:\s*([^;}]+)')
    if (-not $rule.Success) { return 340 }
    $width = $rule.Groups[2].Value.Trim()

    # A root sized from a variable: read what that variable was set to.
    if ($width -match 'var\(\s*(--[\w-]+)') {
        $name = $Matches[1]
        $def = [regex]::Match($css, [regex]::Escape($name) + '\s*:\s*(\d+(?:\.\d+)?)px')
        if ($def.Success) { return [int]$def.Groups[1].Value }
    }
    # Otherwise the last pixel figure in the rule is the ceiling it tops out at.
    $px = [regex]::Matches($width, '(\d+(?:\.\d+)?)px')
    if ($px.Count -gt 0) { return [int]$px[$px.Count - 1].Groups[1].Value }
    return 340
}

# Container query units, resolved to plain pixels.
#
# These designs measure almost everything inside themselves in `cqw` — one percent of
# the widget's own width — which is what lets them hold together at any size. The unit
# only arrived in Chrome 105, though, and a device carrying an older web view than that
# drops every one of these declarations on the floor: the flip clock came out as two
# unstyled digits in the middle of an empty tile.
#
# Since the page is always laid out at the width the design was drawn for, `1cqw` is a
# known number of pixels, so the arithmetic is done here once instead of being asked of
# the browser. Only the root of each design declares `container-type`, and in every
# design that uses the unit that root is exactly [designWidth] across, so the pixel
# value is the one the unit would have resolved to.
function Resolve-ContainerUnits([string]$text, [int]$designWidth) {
    $evaluator = [System.Text.RegularExpressions.MatchEvaluator] {
        param($m)
        $px = [double]$m.Groups[1].Value * $designWidth / 100.0
        [math]::Round($px, 4).ToString([System.Globalization.CultureInfo]::InvariantCulture) + 'px'
    }
    return [regex]::Replace($text, '([\d.]+)cqw\b', $evaluator)
}

# The width to lay the page out at.
#
# Normally the width the design was drawn for. A few of these carry breakpoints of their
# own, though, and a breakpoint at or above that width fires on its own design — the
# analog clocks shrink themselves to `82vw` under 420px, and at their drawn width of 320
# that is always. The result is a widget laid out for a narrow phone where the design
# meant the full-size one, sitting off its own corner.
#
# Laying out just past the breakpoint gives the design the arrangement it was drawn in;
# `embed.js` then scales whatever it settled on up to fill the frame.
function Get-ViewportWidth($cssPath, [int]$designWidth) {
    if (-not (Test-Path $cssPath)) { return $designWidth }
    $css = (Get-Content $cssPath -Encoding UTF8 -Raw) -replace "`r?`n", " "

    $widest = 0
    foreach ($m in [regex]::Matches($css, '@media[^{]*max-width\s*:\s*(\d+)px')) {
        $at = [int]$m.Groups[1].Value
        if ($at -gt $widest) { $widest = $at }
    }
    if ($widest -ge $designWidth) { return $widest + 1 }
    return $designWidth
}

# ---- copy --------------------------------------------------------------------
$catalog = New-Object System.Collections.ArrayList
$fontsSeen = @{}

foreach ($category in $Categories) {
    foreach ($dir in Get-ChildItem (Join-Path $Source $category) -Directory) {
        $infoPath = Join-Path $dir.FullName "widget-info.json"
        if (-not (Test-Path $infoPath)) { continue }
        $info = Read-Json $infoPath
        $id = $dir.Name

        $target = Join-Path $Dest "$category\$id"
        New-Item -ItemType Directory -Path $target -Force | Out-Null

        $designWidth = Get-DesignWidth (Join-Path $dir.FullName "style.css")
        $viewportWidth = Get-ViewportWidth (Join-Path $dir.FullName "style.css") $designWidth

        $scriptSrc = Join-Path $dir.FullName "script.js"
        if (Test-Path $scriptSrc) { Copy-Item $scriptSrc (Join-Path $target "script.js") }

        # The page lays itself out at the width it was drawn for and `embed.js` scales
        # that to whatever frame it is in — it is told the width here. The override
        # stylesheet is hung on the end, after the design's own, so it wins.
        #
        # The fitting is done inside the page rather than left to the view's own zoom
        # because the view will not zoom out past a floor of its own: a 600-wide design
        # asked to fit a thumbnail stopped at 524 and lost its right-hand edge.
        $htmlSrc = Join-Path $dir.FullName "index.html"
        if (Test-Path $htmlSrc) {
            $html = Get-Content $htmlSrc -Encoding UTF8 -Raw
            $html = [regex]::Replace(
                $html,
                '<meta\s+name="viewport"[^>]*>',
                "<meta name=`"viewport`" content=`"width=$viewportWidth`">"
            )
            # `freeze.js` carries no `defer`: it has to have run before the design's own
            # script, which is what it is there to lie to.
            $head = "<style>:root{--design-width:$designWidth}</style>" +
                    "<link rel=`"stylesheet`" href=`"../../embed.css`">" +
                    "<script src=`"../../freeze.js`"></script>" +
                    "<script src=`"../../embed.js`" defer></script></head>"
            $html = $html -replace '</head>', $head
            # A couple of these carry a style block of their own in the page.
            $html = Resolve-ContainerUnits $html $designWidth
            [System.IO.File]::WriteAllText((Join-Path $target "index.html"), $html, (New-Object System.Text.UTF8Encoding $false))
        }

        # Stylesheet, with its private font folder swapped for the shared one.
        $cssSrc = Join-Path $dir.FullName "style.css"
        if (Test-Path $cssSrc) {
            $css = Get-Content $cssSrc -Encoding UTF8 -Raw
            $css = $css -replace 'url\((["'']?)fonts/', 'url($1../../_fonts/'
            $css = Resolve-ContainerUnits $css $designWidth
            [System.IO.File]::WriteAllText((Join-Path $target "style.css"), $css, (New-Object System.Text.UTF8Encoding $false))
        }

        # Only the art a design draws; the originals beside it are not referenced.
        $imagesSrc = Join-Path $dir.FullName "images"
        if (Test-Path $imagesSrc) {
            $webp = Get-ChildItem $imagesSrc -File -Filter *.webp
            if ($webp) {
                $imagesDst = Join-Path $target "images"
                New-Item -ItemType Directory -Path $imagesDst -Force | Out-Null
                $webp | ForEach-Object { Copy-Item $_.FullName (Join-Path $imagesDst $_.Name) }
            }
        }

        # Fonts pool, first one of each name wins -- they are the same file throughout.
        $fontsSrc = Join-Path $dir.FullName "fonts"
        if (Test-Path $fontsSrc) {
            foreach ($f in Get-ChildItem $fontsSrc -File) {
                if (-not $fontsSeen.ContainsKey($f.Name)) {
                    Copy-Item $f.FullName (Join-Path $fontsDir $f.Name)
                    $fontsSeen[$f.Name] = $true
                }
            }
        }

        $all = $allById[$id]
        $cat = $catById[$id]
        if (-not $all) { Write-Warning "no All order for $id"; continue }

        [void]$catalog.Add([ordered]@{
            id          = $id
            name        = $all.name
            category    = $category
            size        = Get-Size $info.size
            cells       = Get-Cells $info.android_cells
            ratio       = Get-Ratio $info.design_ratio
            designWidth = $designWidth
            viewportWidth = $viewportWidth
            allDefault  = $all.default_order
            allLatest   = $all.latest_order
            catDefault  = if ($cat) { $cat.default_order } else { $all.default_order }
            catLatest   = if ($cat) { $cat.latest_order }  else { $all.latest_order }
        })
    }
}

# ---- the shared override ------------------------------------------------------
# Each design ships as a page you can open on its own, so it carries a stand-in
# wallpaper and sits inset in the middle of it. Framed in the app that chrome is in the
# way: the frame is the app's to draw, and the design should fill it edge to edge.
#
# Shadows are left alone. An outer one is clipped by the frame anyway, and some of these
# are lit by inset shadows that are part of how the design looks rather than decoration
# around it.
$embed = @'
html, body {
  margin: 0 !important;
  padding: 0 !important;
  min-height: 0 !important;
  background: transparent !important;
  display: block !important;
  overflow: hidden !important;
}

/* Three designs wrap their widget in a preview frame of their own, and name it
   underneath. The shelf puts the name under the tile itself, so the design's own
   caption would be a second one inside the frame. */
main.preview {
  display: block !important;
  margin: 0 !important;
  padding: 0 !important;
  width: 100% !important;
}

main.preview > .caption {
  display: none !important;
}

/* Nothing is forced on the design itself. These size themselves against the width they
   are given — some of them responsively, with their own breakpoints — and overriding
   that was what left one of them a sliver: its width was pinned while its height went
   on following the variable the design actually sizes from. embed.js measures whatever
   the design settled on and scales that to the frame. */
.widget, .cw, .vw2, .flip, .clock-widget {
  margin: 0 !important;
}
'@
[System.IO.File]::WriteAllText((Join-Path $Dest "embed.css"), $embed, (New-Object System.Text.UTF8Encoding $false))

# Fits the design to the frame.
#
# The design is left to lay itself out however it wants to at the width it has been
# given — several of these are responsive and have breakpoints of their own — and then
# whatever it settled on is measured and scaled to fill the frame exactly. Measuring
# rather than dictating is the point: a design sizes everything inside itself from its
# own width, so a width imposed from outside and a height still following the design's
# own variable do not agree, and the widget comes out a sliver.
#
# The translate puts the widget's own corner at the frame's corner, since a design may
# well be sitting in the middle of a page that is larger than it is.
$embedJs = @'
(function () {
  var ROOTS = '.widget,.cw,.vw2,.flip,.clock-widget';

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
'@
[System.IO.File]::WriteAllText((Join-Path $Dest "embed.js"), $embedJs, (New-Object System.Text.UTF8Encoding $false))

# Holds the clock and the calendar at a given moment.
#
# Half of these designs read a moment off the query string already; the other half just
# ask for the time. Rather than edit twenty-odd scripts, the clock itself is stopped:
# `Date` is replaced before any of them run, so every design is told the same moment
# whichever way it asks. Without a moment on the query string nothing is touched, which
# is how a widget on the home screen keeps real time.
$freezeJs = @'
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
'@
[System.IO.File]::WriteAllText((Join-Path $Dest "freeze.js"), $freezeJs, (New-Object System.Text.UTF8Encoding $false))

$sorted = $catalog | Sort-Object { $_.allDefault }
$json = [ordered]@{ widgets = $sorted } | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText((Join-Path $Dest "catalog.json"), $json, (New-Object System.Text.UTF8Encoding $false))

# ---- report ------------------------------------------------------------------
$size = (Get-ChildItem $Dest -Recurse -File | Measure-Object Length -Sum).Sum
"widgets : {0}" -f $catalog.Count
"fonts   : {0} pooled" -f $fontsSeen.Count
"assets  : {0:N1} MB" -f ($size / 1MB)
