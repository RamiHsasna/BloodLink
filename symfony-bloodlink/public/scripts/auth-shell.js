(function () {
    const root = document.querySelector('[data-auth-shell]');
    if (!root) {
        return;
    }

    const logoCanvas = root.querySelector('[data-auth-logo-canvas]');
    const illustrationCanvas = root.querySelector('[data-auth-illustration-canvas]');

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

    function prepareCanvas(canvas) {
        if (!canvas) {
            return null;
        }
        const ratio = window.devicePixelRatio || 1;
        const rect = canvas.getBoundingClientRect();
        const width = Math.max(1, Math.round(rect.width));
        const height = Math.max(1, Math.round(rect.height));

        if (canvas.width !== Math.round(width * ratio) || canvas.height !== Math.round(height * ratio)) {
            canvas.width = Math.round(width * ratio);
            canvas.height = Math.round(height * ratio);
        }

        const ctx = canvas.getContext('2d');
        if (!ctx) {
            return null;
        }

        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.scale(ratio, ratio);
        return { ctx, width, height };
    }

    function dropX(centerX, radius) {
        const points = 32;
        const xs = new Array(points);
        for (let i = 0; i < points; i += 1) {
            const t = i / points;
            const angle = Math.PI + t * 2 * Math.PI;
            const squeeze = 0.6 + 0.4 * Math.sin(t * Math.PI);
            xs[i] = centerX + Math.cos(angle) * radius * squeeze;
        }
        return xs;
    }

    function dropY(radius, bottom, tipOffset) {
        const points = 32;
        const ys = new Array(points);
        for (let i = 0; i < points; i += 1) {
            const t = i / points;
            const angle = Math.PI + t * 2 * Math.PI;
            const centerY = bottom - radius;
            ys[i] = centerY + Math.sin(angle) * radius - tipOffset * (1 - Math.sin(t * Math.PI));
        }
        return ys;
    }

    function drawLogo() {
        const prepared = prepareCanvas(logoCanvas);
        if (!prepared) {
            return;
        }

        const { ctx, width, height } = prepared;
        ctx.clearRect(0, 0, width, height);

        ctx.fillStyle = '#ffffff33';
        ctx.beginPath();
        ctx.ellipse(width / 2, height / 2, width / 2, height / 2, 0, 0, Math.PI * 2);
        ctx.fill();

        const xs = dropX(width / 2, width * 0.28);
        const ys = dropY(width * 0.28, height * 0.52, height * 0.20);

        ctx.fillStyle = '#ffffffee';
        ctx.beginPath();
        ctx.moveTo(xs[0], ys[0]);
        for (let i = 1; i < xs.length; i += 1) {
            ctx.lineTo(xs[i], ys[i]);
        }
        ctx.closePath();
        ctx.fill();

        ctx.strokeStyle = '#b85c52';
        ctx.lineWidth = 2;
        ctx.lineCap = 'round';
        const centerX = width / 2;
        const centerY = height * 0.62;
        const arm = width * 0.16;
        ctx.beginPath();
        ctx.moveTo(centerX - arm, centerY);
        ctx.lineTo(centerX + arm, centerY);
        ctx.moveTo(centerX, centerY - arm);
        ctx.lineTo(centerX, centerY + arm);
        ctx.stroke();
    }

    function drawRing(ctx, centerX, centerY, radius, color) {
        ctx.strokeStyle = color;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.ellipse(centerX, centerY, radius, radius, 0, 0, Math.PI * 2);
        ctx.stroke();
    }

    function roundedRectPath(ctx, x, y, width, height, radius) {
        const maxR = Math.min(width, height) / 2;
        const r = Math.max(0, Math.min(radius, maxR));

        if (typeof ctx.roundRect === 'function') {
            ctx.roundRect(x, y, width, height, r);
            return;
        }

        ctx.moveTo(x + r, y);
        ctx.lineTo(x + width - r, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + r);
        ctx.lineTo(x + width, y + height - r);
        ctx.quadraticCurveTo(x + width, y + height, x + width - r, y + height);
        ctx.lineTo(x + r, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - r);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
    }

    function drawCurve(ctx, x1, y1, cx, cy, x2, y2) {
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.quadraticCurveTo(cx, cy, x2, y2);
        ctx.stroke();
    }

    function drawMiniDrop(ctx, centerX, centerY, radius) {
        ctx.fillStyle = '#ffc8c878';
        ctx.beginPath();
        ctx.moveTo(centerX, centerY - radius * 1.4);
        ctx.bezierCurveTo(
            centerX + radius,
            centerY - radius * 0.3,
            centerX + radius,
            centerY + radius * 0.5,
            centerX,
            centerY + radius
        );
        ctx.bezierCurveTo(
            centerX - radius,
            centerY + radius * 0.5,
            centerX - radius,
            centerY - radius * 0.3,
            centerX,
            centerY - radius * 1.4
        );
        ctx.closePath();
        ctx.fill();
    }

    function drawDonor(ctx, centerX, centerY) {
        const radius = 18;

        ctx.fillStyle = '#ffffff18';
        ctx.beginPath();
        ctx.ellipse(centerX, centerY, radius, radius, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff38';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        ctx.fillStyle = '#ffc8c8cc';
        const dropRadius = 6;
        ctx.beginPath();
        ctx.ellipse(centerX, centerY, dropRadius, dropRadius, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = '#ffffff28';
        ctx.lineWidth = 1.4;
        ctx.beginPath();
        ctx.ellipse(centerX, centerY + radius + 20, 26, 20, 0, 0, Math.PI);
        ctx.stroke();
    }

    function drawECG(ctx, width, height) {
        const y = height * 0.9;
        const px = [
            width * 0.03,
            width * 0.12,
            width * 0.17,
            width * 0.22,
            width * 0.27,
            width * 0.32,
            width * 0.68,
            width * 0.73,
            width * 0.78,
            width * 0.83,
            width * 0.88,
            width * 0.97
        ];
        const py = [
            y,
            y,
            y - height * 0.08,
            y + height * 0.08,
            y - height * 0.06,
            y,
            y,
            y - height * 0.08,
            y + height * 0.08,
            y - height * 0.06,
            y,
            y
        ];

        ctx.strokeStyle = '#ffffff33';
        ctx.lineWidth = 1.8;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.setLineDash([]);
        ctx.beginPath();
        ctx.moveTo(px[0], py[0]);
        for (let i = 1; i < px.length; i += 1) {
            ctx.lineTo(px[i], py[i]);
        }
        ctx.stroke();
    }

    function drawIllustration() {
        const prepared = prepareCanvas(illustrationCanvas);
        if (!prepared) {
            return;
        }

        const { ctx, width, height } = prepared;
        ctx.clearRect(0, 0, width, height);

        drawRing(ctx, width / 2, height / 2, width * 0.44, '#ffffff0d');
        drawRing(ctx, width / 2, height / 2, width * 0.3, '#ffffff14');

        const hospitalX = width * 0.32;
        const hospitalY = height * 0.22;
        const hospitalW = width * 0.36;
        const hospitalH = height * 0.68;

        ctx.fillStyle = '#ffffff1a';
        ctx.beginPath();
        roundedRectPath(ctx, hospitalX, hospitalY, hospitalW, hospitalH, 10);
        ctx.fill();
        ctx.strokeStyle = '#ffffff40';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        const roofX = hospitalX + hospitalW * 0.12;
        const roofY = hospitalY - height * 0.12;
        const roofW = hospitalW * 0.76;
        const roofH = height * 0.14;

        ctx.fillStyle = '#ffffff10';
        ctx.beginPath();
        roundedRectPath(ctx, roofX, roofY, roofW, roofH, 8);
        ctx.fill();
        ctx.strokeStyle = '#ffffff33';
        ctx.stroke();

        const crossX = roofX + roofW / 2;
        const crossY = roofY + roofH / 2;
        ctx.strokeStyle = '#ffffffdd';
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(crossX - roofW * 0.14, crossY);
        ctx.lineTo(crossX + roofW * 0.14, crossY);
        ctx.moveTo(crossX, crossY - roofH * 0.4);
        ctx.lineTo(crossX, crossY + roofH * 0.4);
        ctx.stroke();

        const windowCols = [hospitalX + hospitalW * 0.12, hospitalX + hospitalW * 0.42, hospitalX + hospitalW * 0.72];
        const windowRows = [hospitalY + hospitalH * 0.2, hospitalY + hospitalH * 0.46];
        const windowW = hospitalW * 0.18;
        const windowH = hospitalH * 0.14;

        for (let row = 0; row < 2; row += 1) {
            for (let col = 0; col < 3; col += 1) {
                const lit = (row + col) % 2 === 0;
                ctx.fillStyle = lit ? '#ffe8a030' : '#ffffff14';
                ctx.beginPath();
                roundedRectPath(ctx, windowCols[col], windowRows[row], windowW, windowH, 4);
                ctx.fill();
                ctx.strokeStyle = '#ffffff2e';
                ctx.lineWidth = 1;
                ctx.stroke();
            }
        }

        const doorX = hospitalX + hospitalW * 0.38;
        const doorY = hospitalY + hospitalH * 0.7;
        const doorW = hospitalW * 0.22;
        const doorH = hospitalH * 0.3;

        ctx.fillStyle = '#ffffff10';
        ctx.beginPath();
        roundedRectPath(ctx, doorX, doorY, doorW, doorH, 6);
        ctx.fill();
        ctx.strokeStyle = '#ffffff35';
        ctx.lineWidth = 1.5;
        ctx.stroke();
        ctx.fillStyle = '#ffffff50';
        ctx.beginPath();
        ctx.ellipse(doorX + doorW * 0.75, doorY + doorH * 0.48, 2.5, 2.5, 0, 0, Math.PI * 2);
        ctx.fill();

        drawDonor(ctx, width * 0.1, height * 0.42);
        drawDonor(ctx, width * 0.86, height * 0.42);

        ctx.strokeStyle = '#ffffff2e';
        ctx.lineWidth = 1.2;
        ctx.setLineDash([6, 4]);
        drawCurve(ctx, width * 0.16, height * 0.56, width * 0.28, height * 0.5, width * 0.32, height * 0.56);
        drawCurve(ctx, width * 0.68, height * 0.56, width * 0.72, height * 0.5, width * 0.84, height * 0.56);
        ctx.setLineDash([]);

        drawMiniDrop(ctx, width * 0.06, height * 0.14, 8);
        drawMiniDrop(ctx, width * 0.9, height * 0.1, 7);
        drawMiniDrop(ctx, width * 0.5, height * 0.04, 9);
        drawMiniDrop(ctx, width * 0.28, height * 0.08, 6);
        drawMiniDrop(ctx, width * 0.72, height * 0.06, 6);

        drawECG(ctx, width, height);
    }

    function renderHeroArt() {
        drawLogo();
        drawIllustration();
    }

    function renderHeroArtWithRetry() {
        renderHeroArt();

        // In some layouts, canvas width is 0 on first paint; retry once the frame settles.
        window.requestAnimationFrame(renderHeroArt);
        window.setTimeout(renderHeroArt, 120);
    }

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

    renderHeroArtWithRetry();
    window.addEventListener('resize', renderHeroArtWithRetry);
    window.addEventListener('load', renderHeroArtWithRetry);
})();
