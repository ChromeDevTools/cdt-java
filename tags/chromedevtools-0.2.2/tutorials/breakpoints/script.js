window.addEventListener("load", onLoad, true);
document.addEventListener("mouseover", onMouseOver, true);

function onLoad() {
  var hovermeElement = document.getElementById("hoverme");
  hovermeElement.addEventListener("mouseover", hovermeMouseOver, true);
  hovermeElement.addEventListener("mouseout", hovermeMouseOut, true);
}

function hovermeMouseOver(event) {
  event.target.style.backgroundColor = "grey";
}

function hovermeMouseOut(event) {
  event.target.style.backgroundColor = "white";
}

function onMouseOver(event) {
  var target = event.target;
  return "onMouseOver: " + target;
}

function raiseAndCatchException() {
  var element = document.createElement("div");
  try {
    document.body.appendChild(elemetn);
  } catch(e) {
    console.log(e);
  }
}

function raiseException() {
  throw 0;
}

function loadDynamicScript() {
  var request = new XMLHttpRequest();
  request.open('GET','dynamicScript.js', true);
  request.send();
  request.onreadystatechange = function() {
    if (request.readyState != 4)
      return;
    eval(request.responseText);
    document.getElementById("dynamicScriptFunctionButton").disabled = false;
    document.getElementById("loadDynamicScriptButton").disabled = true;
  }
}

function appendChildButtonClicked() {
  var parentElement = document.getElementById("parent");
  var childElement = document.createElement("div");
  childElement.className = "test-element";
  childElement.style.width = "120px";
  childElement.textContent = "Child Element";
  parentElement.appendChild(childElement);
}

function retrieveData() {
  var request = new XMLHttpRequest();
  request.open('GET','data.txt', true);
  request.send();
}
