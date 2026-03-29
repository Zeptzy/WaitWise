
let currentUser    = null;
let googleAccounts = [];

const MOCK_QUEUE = [
  { num: 'A001', type: 'Document Request', name: 'Student Name', waitMin: 5,  status: 'waiting' },
];

// Transaction type → estimated minutes per student
const WAIT_RATES = {
  'Document Request': 5,
  'Enrollment':       10,
  'Course Concern':   7,
  'Graduation':       12,
};

/* ── INIT ── */
document.addEventListener('DOMContentLoaded', () => {
  startLoadingScreen();
  loadGoogleAccounts();
  checkRememberedUser();
  attachLiveValidation();
});

/* ── LOADING SCREEN ── */
function startLoadingScreen() {
  setTimeout(() => {
    const screen = document.getElementById('loadingScreen');
    screen.style.opacity = '0';
    setTimeout(() => {
      screen.style.display = 'none';
      document.getElementById('mainWrapper').style.display = 'flex';
    }, 500);
  }, 1800);
}

/* ── CARD SWITCHING ── */
function switchCard(cardId) {
  const cards = ['welcomeCard','loginCard','signupCard','loggedInCard','forgotCard','resetSentCard'];
  cards.forEach(id => {
    const el = document.getElementById(id);
    if (el) { el.classList.remove('active'); el.style.display = ''; }
  });
  const target = document.getElementById(cardId);
  if (target) target.classList.add('active');

  // Clear alerts
  ['loginAlert','signupAlert','forgotAlert'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
  });
}

/* ── SHOW ALERT ── */
function showAlert(alertId, message, type) {
  const el = document.getElementById(alertId);
  if (!el) return;
  const icons = { success: '<i class="fas fa-check-circle"></i>', error: '<i class="fas fa-exclamation-circle"></i>' };
  el.className = `alert-box ${type}`;
  el.innerHTML = `${icons[type] || ''} ${message}`;
  el.style.display = 'flex';
  clearTimeout(el._timer);
  el._timer = setTimeout(() => { el.style.display = 'none'; }, 5000);
}

/* ── FIELD ERROR HELPERS ── */
function showFieldError(inputId, message) {
  const input = document.getElementById(inputId);
  if (!input) return;
  input.classList.add('field-error');
  const wrap = input.closest('.field-wrap') || input.parentElement;
  let helper = wrap.parentElement.querySelector('.field-helper');
  if (!helper) {
    helper = document.createElement('p');
    helper.className = 'field-helper';
    wrap.parentElement.appendChild(helper);
  }
  helper.textContent = message;
  helper.style.display = 'flex';
}

function clearFieldError(inputId) {
  const input = document.getElementById(inputId);
  if (!input) return;
  input.classList.remove('field-error');
  const wrap = input.closest('.field-wrap') || input.parentElement;
  const helper = wrap.parentElement.querySelector('.field-helper');
  if (helper) helper.style.display = 'none';
}

function clearAllSignupErrors() {
  ['signupName','signupId','signupCourse','signupEmail','signupPhone','signupPassword','signupConfirm']
    .forEach(id => clearFieldError(id));
  // Also clear terms error
  const te = document.getElementById('termsError');
  if (te) te.style.display = 'none';
}

/* ── LIVE VALIDATION ── */
function attachLiveValidation() {
  const fields = ['signupName','signupId','signupCourse','signupEmail','signupPhone','signupPassword','signupConfirm'];
  fields.forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener('input',  () => clearFieldError(id));
      el.addEventListener('change', () => clearFieldError(id));
    }
  });
  // Clear terms error when checkbox changes
  const terms = document.getElementById('agreeTerms');
  if (terms) terms.addEventListener('change', () => {
    const te = document.getElementById('termsError');
    if (te) te.style.display = 'none';
  });
}

/* ── HANDLE LOGIN ── */
function handleLogin(event) {
  event.preventDefault();

  const email    = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  const remember = document.getElementById('rememberMe').checked;

  const users = JSON.parse(localStorage.getItem('ww_users') || '[]');
  const user  = users.find(u => u.email === email && u.password === password);

  if (user) {
    currentUser = user;

    // Remember Me: save email to localStorage so we can pre-fill it next visit
    if (remember) {
      localStorage.setItem('ww_remembered_email', email);
    } else {
      localStorage.removeItem('ww_remembered_email');
    }

    showLoggedIn(user);
  } else {
    showAlert('loginAlert', 'Invalid email or password. Please try again.', 'error');
  }
}

/* ── REMEMBER ME: pre-fill email on page load ── */
function checkRememberedUser() {
  const saved = localStorage.getItem('ww_remembered_email');
  if (saved) {
    const emailInput = document.getElementById('loginEmail');
    const rememberCb = document.getElementById('rememberMe');
    if (emailInput) emailInput.value = saved;
    if (rememberCb) rememberCb.checked = true;
  }
}

/* ── FORGOT PASSWORD ── */
function handleForgotPassword(event) {
  event.preventDefault();

  const email = document.getElementById('forgotEmail').value.trim();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!email) {
    showAlert('forgotAlert', 'Please enter your email address.', 'error');
    return;
  }
  if (!emailRegex.test(email)) {
    showAlert('forgotAlert', 'Please enter a valid email address.', 'error');
    return;
  }

  // Check if this email exists in the registered users
  const users = JSON.parse(localStorage.getItem('ww_users') || '[]');
  const exists = users.some(u => u.email.toLowerCase() === email.toLowerCase());

  if (!exists) {
    showAlert('forgotAlert', 'No account found with this email address.', 'error');
    return;
  }

  // Update the "sent to" message on the next card
  const sub = document.getElementById('resetSentSub');
  if (sub) sub.textContent = `We've sent a password reset link to ${email}. Please check your inbox.`;
  document.getElementById('forgotForm').reset();
  switchCard('resetSentCard');
}

/* ── HANDLE SIGN-UP ── */
function handleSignup(event) {
  event.preventDefault();
  clearAllSignupErrors();

  const name     = document.getElementById('signupName').value.trim();
  const id       = document.getElementById('signupId').value.trim();
  const course   = document.getElementById('signupCourse').value;
  const email    = document.getElementById('signupEmail').value.trim();
  const phone    = document.getElementById('signupPhone').value.trim();
  const password = document.getElementById('signupPassword').value;
  const confirm  = document.getElementById('signupConfirm').value;
  const agreed   = document.getElementById('agreeTerms').checked;

  let hasError = false;

  // Full Name
  if (!name) {
    showFieldError('signupName', 'Full name is required.');
    hasError = true;
  } else if (!/^[A-Za-zÀ-ÿ\s.'\-]+$/.test(name)) {
    showFieldError('signupName', 'Name must contain letters only (no numbers or symbols).');
    hasError = true;
  } else if (name.split(/\s+/).filter(w => w.length > 0).length < 2) {
    showFieldError('signupName', 'Please enter your full name (first and last name).');
    hasError = true;
  }

  // Student ID — must be numbers only (enforced in HTML too, double-checked here)
  if (!id) {
    showFieldError('signupId', 'Student ID is required.');
    hasError = true;
  } else if (!/^\d+$/.test(id)) {
    // This catches any edge case where non-digits somehow got in (e.g. paste on some browsers)
    showFieldError('signupId', 'Student ID must contain numbers only — no letters or symbols.');
    hasError = true;
  } else if (id.length < 4) {
    showFieldError('signupId', 'Student ID is too short. Please double-check.');
    hasError = true;
  }

  // Course
  if (!course) {
    showFieldError('signupCourse', 'Please select your course.');
    hasError = true;
  }

  // Email
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!email) {
    showFieldError('signupEmail', 'Email address is required.');
    hasError = true;
  } else if (!emailRegex.test(email)) {
    showFieldError('signupEmail', 'Please enter a valid email address (e.g. you@email.com).');
    hasError = true;
  }

  // Phone — Philippine format: starts with 09, exactly 11 digits
  const rawPhone   = phone.replace(/[\s\-]/g, '');
  const phoneRegex = /^09\d{9}$/;
  if (!phone) {
    showFieldError('signupPhone', 'Phone number is required.');
    hasError = true;
  } else if (/[a-zA-Z]/.test(phone)) {
    showFieldError('signupPhone', 'Phone number must contain digits only — no letters.');
    hasError = true;
  } else if (!phoneRegex.test(rawPhone)) {
    showFieldError('signupPhone', 'Enter a valid PH mobile number starting with 09 (e.g. 09XX-XXX-XXXX).');
    hasError = true;
  }

  // Password
  if (!password) {
    showFieldError('signupPassword', 'Password is required.');
    hasError = true;
  } else if (password.length < 8) {
    showFieldError('signupPassword', 'Password must be at least 8 characters long.');
    hasError = true;
  } else if (!/\d/.test(password)) {
    showFieldError('signupPassword', 'Password must include at least one number.');
    hasError = true;
  }

  // Confirm password
  if (!confirm) {
    showFieldError('signupConfirm', 'Please confirm your password.');
    hasError = true;
  } else if (confirm !== password) {
    showFieldError('signupConfirm', 'Passwords do not match.');
    hasError = true;
  }

  // Terms & Conditions checkbox
  if (!agreed) {
    const te = document.getElementById('termsError');
    if (te) te.style.display = 'flex';
    hasError = true;
  }

  if (hasError) {
    showAlert('signupAlert', 'Please fix the highlighted errors below.', 'error');
    return;
  }

  // Duplicate email check
  const users = JSON.parse(localStorage.getItem('ww_users') || '[]');
  if (users.some(u => u.email.toLowerCase() === email.toLowerCase())) {
    showFieldError('signupEmail', 'This email is already registered.');
    showAlert('signupAlert', 'This email is already registered. Please sign in instead.', 'error');
    return;
  }

  // Save new user (store cleaned phone and numeric-only ID)
  const newUser = { name, studentId: id, course, email, phone: rawPhone, password };
  users.push(newUser);
  localStorage.setItem('ww_users', JSON.stringify(users));

  showAlert('signupAlert', '✅ Account created successfully! Redirecting to sign in…', 'success');
  document.getElementById('signupForm').reset();
  clearAllSignupErrors();
  setTimeout(() => switchCard('loginCard'), 1800);
}

/* ── SHOW LOGGED-IN STATE ── */
function showLoggedIn(user) {
  const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  const firstName = user.name.split(' ')[0];

  document.getElementById('loggedInAvatar').textContent = initials;
  document.getElementById('loggedInName').textContent   = `Welcome, ${firstName}!`;
  document.getElementById('loggedInSub').textContent    = 'You are now signed in to WaitWise.';

  // Student ID is already pure numbers for new accounts.
  // For old Google-created accounts that might have "G-XXXXX", strip the prefix.
  const cleanId = (user.studentId || '').replace(/^[A-Za-z\-]+/, '') || user.studentId || '—';

  const infoEl = document.getElementById('loggedInInfo');
  const rows = [
    { label: 'Full Name',   value: user.name },
    { label: 'Student ID',  value: cleanId },
    { label: 'Course',      value: user.course || '—' },
    { label: 'Email',       value: user.email },
  ];
  infoEl.innerHTML = rows.map(r => `
    <div class="info-row">
      <span class="ir-label">${r.label}</span>
      <span class="ir-val">${r.value}</span>
    </div>
  `).join('');

  // Render queue status
  renderQueueStatus(user);

  switchCard('loggedInCard');

  // Clear login fields for security
  document.getElementById('loginEmail').value    = '';
  document.getElementById('loginPassword').value = '';
}

/* ── QUEUE STATUS SECTION ──
   Shows the user's current queue ticket, request type, and wait time.
   In a real app, you'd fetch this from your Laravel API.
   Here we show mock data + calculate a dynamic wait time.
──────────────────────────────────────────────────────────────── */
function renderQueueStatus(user) {
  const section = document.getElementById('queueStatusSection');
  const body    = document.getElementById('queueStatusBody');

  // Simulate: user has a queue ticket if they've ever logged in
  // In production, replace with: const queue = await fetch(`/api/queue/${user.studentId}`)
  const hasQueue = true; // change to false to show the empty state

  if (!hasQueue || !MOCK_QUEUE.length) {
    section.style.display = 'block';
    body.innerHTML = `
      <div class="qs-empty">
        <i class="fas fa-clock"></i>
        No active queue. <a href="#" class="switch-link" style="font-size:.88rem;">Join a queue</a> from the mobile app.
      </div>`;
    return;
  }

  section.style.display = 'block';

  body.innerHTML = MOCK_QUEUE.map(q => {
    // Calculate a realistic wait time:
    // (position in queue - 1) × minutes per transaction type
    const positionInLine = 1; // first in line in mock data
    const rate    = WAIT_RATES[q.type] || 5;
    const waitMin = positionInLine === 1 ? rate : (positionInLine - 1) * rate;

    const statusLabel = {
      waiting: 'Waiting',
      serving: 'Now Serving',
      skipped: 'Skipped',
    }[q.status] || 'Waiting';

    return `
      <div class="qs-item">
        <div class="qs-num">${q.num}</div>
        <div class="qs-details">
          <div class="qs-type">${q.type}</div>
          <div class="qs-name">${user.name}</div>
        </div>
        <div class="qs-right">
          <div class="qs-wait">~${waitMin} min</div>
          <div class="qs-wait-label">est. wait</div>
          <span class="qs-status ${q.status}">${statusLabel}</span>
        </div>
      </div>`;
  }).join('');
}

/* ── LOGOUT ── */
function logout() {
  currentUser = null;
  switchCard('welcomeCard');
}

/* ── PASSWORD TOGGLE ── */
function togglePw(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const isHidden = input.type === 'password';
  input.type = isHidden ? 'text' : 'password';
  btn.innerHTML = isHidden ? '<i class="fas fa-eye-slash"></i>' : '<i class="fas fa-eye"></i>';
}

/* ── GOOGLE ACCOUNTS ── */
function loadGoogleAccounts() {
  const saved = localStorage.getItem('ww_google_accounts');
  if (saved) {
    googleAccounts = JSON.parse(saved);
  } else {
    googleAccounts = [
      { name: 'Khujei Ken Amarila',  email: 'khujei@gmail.com',    avatar: 'K' },
      { name: 'Steve Allen Montero', email: 'steveallen@gmail.com', avatar: 'S' },
    ];
    saveGoogleAccounts();
  }
}
function saveGoogleAccounts() { localStorage.setItem('ww_google_accounts', JSON.stringify(googleAccounts)); }

function getAvatarColor(email) {
  const palette = ['#1a73e8','#34a853','#f9a825','#ea4335','#7c4dff','#00acc1'];
  let hash = 0;
  for (let i = 0; i < email.length; i++) hash = email.charCodeAt(i) + ((hash << 5) - hash);
  return palette[Math.abs(hash) % palette.length];
}

/* ── GOOGLE MODAL ── */
function openGoogleModal() {
  renderGoogleAccounts();
  document.getElementById('googleModal').classList.add('open');
}
function closeGoogleModal(event) {
  if (event && event.target !== document.getElementById('googleModal')) return;
  document.getElementById('googleModal').classList.remove('open');
}
function renderGoogleAccounts() {
  const container = document.getElementById('gmAccounts');
  if (!googleAccounts.length) {
    container.innerHTML = `<p style="text-align:center;padding:1.5rem;color:#5f6368;font-size:.88rem;">No saved accounts</p>`;
    return;
  }
  container.innerHTML = googleAccounts.map(acc => `
    <div class="gm-account" onclick="selectGoogleAccount('${acc.email}')">
      <div class="gm-avatar" style="background:${getAvatarColor(acc.email)}">${acc.avatar}</div>
      <div class="gm-info">
        <div class="gm-name">${acc.name}</div>
        <div class="gm-email">${acc.email}</div>
      </div>
    </div>
  `).join('');
}

function selectGoogleAccount(email) {
  const acc = googleAccounts.find(a => a.email === email);
  if (!acc) return;
  closeGoogleModal();

  const users = JSON.parse(localStorage.getItem('ww_users') || '[]');
  let user = users.find(u => u.email === email);

  if (!user) {
    // Auto-generate a numeric student ID for Google accounts
    const numericId = String(Math.floor(Math.random() * 900000000 + 100000000));
    user = { name: acc.name, studentId: numericId, course: 'BSIT', email: acc.email, phone: 'Not provided', password: 'google-auth' };
    users.push(user);
    localStorage.setItem('ww_users', JSON.stringify(users));
  }

  currentUser = user;
  showLoggedIn(user);
}

function addGoogleAccount() {
  const name = prompt('Enter the full name for this Google account:');
  if (!name || !name.trim()) return;
  const email = prompt('Enter the Gmail address:');
  if (!email || !email.includes('@')) { alert('Please enter a valid email address.'); return; }
  if (googleAccounts.some(a => a.email === email)) { alert('This account is already in the list.'); return; }
  googleAccounts.push({ name: name.trim(), email: email.trim(), avatar: name.trim().charAt(0).toUpperCase() });
  saveGoogleAccounts();
  renderGoogleAccounts();
}

/* ── TERMS & PRIVACY MODALS ── */
function openTermsModal() {
  document.getElementById('termsModal').classList.add('open');
}
function closeTermsModal(event) {
  if (event && event.target !== document.getElementById('termsModal')) return;
  document.getElementById('termsModal').classList.remove('open');
}
function openPrivacyModal() {
  document.getElementById('privacyModal').classList.add('open');
}
function closePrivacyModal(event) {
  if (event && event.target !== document.getElementById('privacyModal')) return;
  document.getElementById('privacyModal').classList.remove('open');
}
