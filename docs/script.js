const revealItems = document.querySelectorAll('.reveal');
const testCheckout = document.querySelector('#test-checkout');
const testSaleButton = document.querySelector('#test-sale-button');
const testCloseButton = document.querySelector('#test-close-button');
const testCheckoutForm = document.querySelector('#test-checkout-form');
const testResult = document.querySelector('#test-result');

const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.12 });

revealItems.forEach((item) => observer.observe(item));

const closeTestCheckout = () => {
  testCheckout.hidden = true;
  document.body.style.overflow = '';
};

testSaleButton.addEventListener('click', () => {
  testCheckout.hidden = false;
  document.body.style.overflow = 'hidden';
  testCheckout.querySelector('input').focus();
});

testCloseButton.addEventListener('click', closeTestCheckout);

testCheckout.addEventListener('click', (event) => {
  if (event.target === testCheckout) closeTestCheckout();
});

testCheckoutForm.addEventListener('submit', (event) => {
  event.preventDefault();
  testResult.hidden = false;
  testCheckoutForm.reset();
});