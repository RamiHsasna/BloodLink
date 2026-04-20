(function () {
    const root = document.querySelector('[data-auth-shell]');
    if (!root) {
        return;
    }

    const initialTab = root.dataset.initialTab === 'signup' ? 'signup' : 'signin';
    const hasStep1ServerErrors = root.dataset.hasStep1Errors === '1';

    const tabSignIn = document.getElementById('tab-signin');
    const tabSignUp = document.getElementById('tab-signup');
    const paneSignIn = document.getElementById('pane-signin');
    const paneSignUp = document.getElementById('pane-signup');
    const signupStep0 = document.getElementById('signup-step-0');
    const signupStep1 = document.getElementById('signup-step-1');
    const signupDot0 = document.getElementById('signup-dot-0');
    const signupDot1 = document.getElementById('signup-dot-1');
    const signupNext = document.getElementById('signup-next');
    const signupBack = document.getElementById('signup-back');

    function setSignupStep(step) {
        const isStep0 = step === 0;
        if (signupStep0) {
            signupStep0.classList.toggle('active', isStep0);
        }
        if (signupStep1) {
            signupStep1.classList.toggle('active', !isStep0);
        }
        if (signupDot0) {
            signupDot0.classList.toggle('active', isStep0);
        }
        if (signupDot1) {
            signupDot1.classList.toggle('active', !isStep0);
        }
    }

    function setError(id, message) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }
        element.textContent = message || '';
    }

    function validateSignupStep0() {
        const firstName = document.getElementById('first_name').value.trim();
        const lastName = document.getElementById('last_name').value.trim();
        const email = document.getElementById('email').value.trim();
        const bloodType = document.getElementById('blood_type_id').value.trim();

        let valid = true;

        setError('err-first-name', '');
        setError('err-last-name', '');
        setError('err-email', '');
        setError('err-blood-type', '');

        if (!firstName) {
            setError('err-first-name', 'First name is required.');
            valid = false;
        }
        if (!lastName) {
            setError('err-last-name', 'Last name is required.');
            valid = false;
        }
        if (!email) {
            setError('err-email', 'Email is required.');
            valid = false;
        } else {
            const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
            if (!emailRegex.test(email)) {
                setError('err-email', 'Please enter a valid email address.');
                valid = false;
            }
        }
        if (!bloodType) {
            setError('err-blood-type', 'Please select a blood type.');
            valid = false;
        }

        return valid;
    }

    function activate(tab) {
        const signin = tab === 'signin';
        if (tabSignIn) {
            tabSignIn.classList.toggle('active', signin);
        }
        if (tabSignUp) {
            tabSignUp.classList.toggle('active', !signin);
        }
        if (paneSignIn) {
            paneSignIn.classList.toggle('active', signin);
        }
        if (paneSignUp) {
            paneSignUp.classList.toggle('active', !signin);
        }
        if (!signin) {
            setSignupStep(0);
        }
    }

    if (tabSignIn) {
        tabSignIn.addEventListener('click', function () {
            activate('signin');
        });
    }
    if (tabSignUp) {
        tabSignUp.addEventListener('click', function () {
            activate('signup');
        });
    }

    const switchToSignup = document.getElementById('switch-to-signup');
    const switchToSignin = document.getElementById('switch-to-signin');

    if (switchToSignup) {
        switchToSignup.addEventListener('click', function () {
            activate('signup');
        });
    }

    if (switchToSignin) {
        switchToSignin.addEventListener('click', function () {
            activate('signin');
        });
    }

    if (signupNext) {
        signupNext.addEventListener('click', function () {
            if (validateSignupStep0()) {
                setSignupStep(1);
            }
        });
    }

    if (signupBack) {
        signupBack.addEventListener('click', function () {
            setSignupStep(0);
        });
    }

    activate(initialTab === 'signup' ? 'signup' : 'signin');
    if (initialTab === 'signup' && hasStep1ServerErrors) {
        setSignupStep(1);
    }
})();
