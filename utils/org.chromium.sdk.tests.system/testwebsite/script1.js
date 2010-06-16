// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

counter = 0;

function PeriodicRun() {
  var x = 1;
  var y = 2;
  // #breakpoint#1#
  var sum = x + y;

  SetStatus(counter++);
  
  return sum;
}

function Fibonacci(number) {
  var res = 1;
  while (number > 1) {
    res = res * number;
    number--;
  }
  return res;
}

periodic_run_id = window.setInterval(PeriodicRun, 1000);