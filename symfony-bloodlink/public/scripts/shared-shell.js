(function () {
    const modal = document.querySelector('[data-confirm-modal]');
    if (!modal) {
        return;
    }

    const backdrop = modal.querySelector('[data-confirm-backdrop]');
    const copy = modal.querySelector('[data-confirm-copy]');
    const cancel = modal.querySelector('[data-confirm-cancel]');
    const submit = modal.querySelector('[data-confirm-submit]');
    let activeForm = null;

    function closeModal() {
        activeForm = null;
        modal.hidden = true;
        modal.removeAttribute('data-confirm-open');
    }

    function openModal(form, message, submitLabel) {
        activeForm = form;
        if (copy) {
            copy.textContent = message || 'Confirm this action.';
        }
        if (submit) {
            submit.textContent = submitLabel || 'Delete';
        }
        modal.hidden = false;
        modal.setAttribute('data-confirm-open', 'true');
    }

    document.addEventListener('submit', function (event) {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        const message = form.dataset.confirmMessage;
        if (!message || form.dataset.confirmBypass === '1') {
            return;
        }

        event.preventDefault();
        openModal(form, message, form.dataset.confirmLabel || 'Delete');
    });

    if (cancel) {
        cancel.addEventListener('click', closeModal);
    }

    if (backdrop) {
        backdrop.addEventListener('click', closeModal);
    }

    if (submit) {
        submit.addEventListener('click', function () {
            if (!activeForm) {
                closeModal();
                return;
            }

            activeForm.dataset.confirmBypass = '1';
            activeForm.submit();
            closeModal();
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !modal.hidden) {
            closeModal();
        }
    });
})();
