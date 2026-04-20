(function () {
    const modal = document.querySelector('[data-confirm-modal]');
    if (!modal) {
        return;
    }

    const copy = modal.querySelector('[data-confirm-copy]');
    const cancelButton = modal.querySelector('[data-confirm-cancel]');
    const submitButton = modal.querySelector('[data-confirm-submit]');
    const backdrop = modal.querySelector('[data-confirm-backdrop]');

    let pendingForm = null;
    let lastFocusedElement = null;

    const closeModal = () => {
        modal.hidden = true;
        document.body.classList.remove('bo-confirm-open');
        pendingForm = null;

        if (lastFocusedElement instanceof HTMLElement) {
            lastFocusedElement.focus();
        }
    };

    const openModal = (form) => {
        pendingForm = form;
        lastFocusedElement = document.activeElement;
        copy.textContent = form.dataset.confirmMessage || 'Confirm this action.';
        modal.hidden = false;
        document.body.classList.add('bo-confirm-open');
        submitButton.focus();
    };

    document.addEventListener(
        'submit',
        (event) => {
            const form = event.target;
            if (!(form instanceof HTMLFormElement)) {
                return;
            }

            if (!form.dataset.confirmMessage) {
                return;
            }

            if (form.dataset.confirmed === '1') {
                delete form.dataset.confirmed;
                return;
            }

            event.preventDefault();
            openModal(form);
        },
        true,
    );

    submitButton.addEventListener('click', () => {
        if (!pendingForm) {
            closeModal();
            return;
        }

        pendingForm.dataset.confirmed = '1';
        if (typeof pendingForm.requestSubmit === 'function') {
            pendingForm.requestSubmit();
        } else {
            pendingForm.submit();
        }

        closeModal();
    });

    cancelButton.addEventListener('click', closeModal);
    backdrop.addEventListener('click', closeModal);

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !modal.hidden) {
            event.preventDefault();
            closeModal();
        }
    });
})();
