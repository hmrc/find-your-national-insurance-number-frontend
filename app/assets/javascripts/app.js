// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
  window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

  // handle exclusive checkbox
  var checkboxes = document.querySelectorAll('.govuk-checkboxes__input');
  var exclusiveCheckbox = document.querySelector('[data-behaviour="exclusive"]');
  if (exclusiveCheckbox !== null) {
     checkboxes.forEach(function (checkbox) {
        checkbox.addEventListener('click', function() {
           if (checkbox === exclusiveCheckbox) {
              checkboxes.forEach(function (c) {
                 if (c !== exclusiveCheckbox) {
                    c.checked = false;
                 }
              });
           } else {
              exclusiveCheckbox.checked = false;
           }
        });
     });
  }
});

const printLink = document.getElementById('print-this');

if(printLink != null && printLink != 'undefined' ) {
    printLink.addEventListener("click", function (e) {
        e.preventDefault();
        window.print();
    });
};