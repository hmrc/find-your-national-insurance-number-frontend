// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
  window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

  // handle back click
  var backLink = document.querySelector('.govuk-back-link');
  if (backLink !== null) {
    backLink.addEventListener('click', function(e){
      // use browser back for default back links only (ending in #)
      if (backLink.href.endsWith('#')) {
        e.preventDefault();
        e.stopPropagation();
        window.history.back();
      }
    });
  }
});