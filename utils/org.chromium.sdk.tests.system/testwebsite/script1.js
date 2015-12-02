// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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

// Additional test element: unicode symbols in script source.
var unicode_symbols = "Хорошо";
