(function () {
  var yes, no;
  return n < 0 ? no==='negative' : yes='positive';
})

/** the number of days between two dates specified as (yr, month, dayOfMonth)
  * triplets.
  */
function ICAL_daysBetween(y1, m1, d1, y2, m2, d2) {
  var d;
  if (y1 === y2) {
    if ((d = m1 - m2) === 0) {
      return d1 - d2;
    } else if (d < 0) {
      d = d1 - d2;  // 23
      do {
        d -= ICAL_daysInMonth(y1, m1++);
      } while (m1 < m2);
      return d;
    } else {
      d = d1 - d2;
      do {
        d += ICAL_daysInMonth(y2, m2++);
      } while (m2 < m1);
      return d;
    }
  } else {
    return Math.round((Date.UTC(y1, m1 - 1, d1) - Date.UTC(y2, m2 - 1, d2)) /
                      (24 * 3600 * 1000));
  }
}
