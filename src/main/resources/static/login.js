const params = new URLSearchParams(location.search);
const message = document.querySelector('#message');

if (params.get('error') === 'true') {
  message.textContent = 'Invalid username or password.';
  message.classList.remove('hidden');
}

if (params.get('logout') === 'true') {
  message.textContent = 'You have been securely signed out.';
  message.className = 'message info';
}
