/*! Vision Widget — Analog clock : 시·분침, 디지털 시, 요일/날짜 실시간 갱신 */
(function () {
  const hourHand = document.getElementById("hour");
  const minuteHand = document.getElementById("minute");
  const digitalHour = document.getElementById("digital-hour");
  const dayEl = document.getElementById("day");
  const dateEl = document.getElementById("date");

  function pad(n) { return String(n).padStart(2, "0"); }

  function updateClock() {
    const now = new Date();
    const seconds = now.getSeconds() + now.getMilliseconds() / 1000;
    const minutes = now.getMinutes() + seconds / 60;
    const hours = (now.getHours() % 12) + minutes / 60;

    hourHand.style.transform =
      "translateX(-50%) rotate(" + (hours * 30) + "deg)";
    minuteHand.style.transform =
      "translateX(-50%) rotate(" + (minutes * 6) + "deg)";

    digitalHour.textContent = String(now.getHours() % 12 || 12);

    dayEl.textContent = now.toLocaleDateString("en-US", { weekday: "short" }).toUpperCase();
    dateEl.textContent = now.toLocaleDateString("en-US", {
      month: "short",
      day: "2-digit"
    }).toUpperCase();

    requestAnimationFrame(updateClock);
  }

  updateClock();
})();