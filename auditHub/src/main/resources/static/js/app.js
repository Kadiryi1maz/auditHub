/* ============================================================
   auditHub — app.js
   - Canlı JQL preview
   - Tarih aralığı client-side kontrolü
   - Submit loading state
   ============================================================ */

(function () {
    'use strict';

    // ---------- Element referansları ----------
    const form                   = document.getElementById('exportForm');
    const mudurlukInput          = document.getElementById('mudurluk');
    const createdStartDateInput  = document.getElementById('createdStartDate');
    const statusChangedStartInput = document.getElementById('statusChangedStartDate');
    const statusChangedEndInput  = document.getElementById('statusChangedEndDate');
    const jqlPreviewEl           = document.getElementById('jqlPreview');
    const submitBtn              = document.getElementById('submitBtn');
    const btnText                = document.getElementById('btnText');
    const btnLoading             = document.getElementById('btnLoading');

    // ---------- JQL Preview ----------

    function getCheckedValues(name) {
        return Array.from(document.querySelectorAll(`input[name="${name}"]:checked`))
            .map(cb => cb.value);
    }

    function buildJqlPreview() {
        const mudurluk   = mudurlukInput ? mudurlukInput.value.trim() : '';
        const projects   = {
            'Açık Sistem Çöz. Md.': ['FTBASM', 'MSS', 'MES'],
            'Rezervasyon & Biletleme Md.': ['TRP', 'TKT'],
            'Gelir Yönetimi ve Ücret Çözümleri Md.': ['PRC', 'RMOP', 'PTS', 'UVYFT'],
            'Doğrudan Satış Çözümleri Md.': ['NDCIN', 'NDCUI', 'QR', 'KB'],
            'Dijital Yolcu Çöz Md.': ['QCG', 'KIOSK', 'DYMU', 'OTHL', 'SBD', 'TOUR'],
            'DCS Çözümleri Md.': ['TKP4089', 'DCWB', 'TDCS'],
            'Alışveriş İçerik Md.': ['DIJITAL'],
            'Miles&Smiles Md.': ['DIJITAL'],
            'Biletleme ve Ek Hizmetler Md.': ['DIJITAL'],
            'Satış Sonrası ve IRROPS Md.': ['DIJITAL']
        };
        const projectList = projects[mudurluk] || [mudurluk];
        const issueTypes = getCheckedValues('issueTypes');
        const statuses   = getCheckedValues('statuses');
        const createdStart       = createdStartDateInput ? createdStartDateInput.value : '';
        const changedStart       = statusChangedStartInput ? statusChangedStartInput.value : '';
        const changedEnd         = statusChangedEndInput ? statusChangedEndInput.value : '';

        if (!mudurluk || issueTypes.length === 0 || statuses.length === 0
                || !createdStart || !changedStart || !changedEnd) {
            if (jqlPreviewEl) {
                jqlPreviewEl.textContent = 'Tüm alanları doldurunca JQL burada görünecek...';
            }
            return;
        }

        const issueTypePart  = issueTypes.join(', ');
        const statusPart     = statuses.join(', ');
        const projectPart    = projectList.map(p => `"${p}"`).join(', ');

        let jql = `issuetype in (${issueTypePart})`
            + ` AND created >= "${createdStart}"`
            + ` AND project in (${projectPart})`
            + ` AND status changed to (${statusPart})`
            + ` during ("${changedStart}", "${changedEnd}")`;

        if (jqlPreviewEl) {
            jqlPreviewEl.textContent = jql;
        }
    }

    // ---------- Tarih aralığı client-side kontrolü ----------

    function validateDateRange() {
        if (!statusChangedStartInput || !statusChangedEndInput) return true;

        const start = statusChangedStartInput.value;
        const end   = statusChangedEndInput.value;

        if (start && end && end < start) {
            statusChangedEndInput.classList.add('input-error');
            return false;
        }

        statusChangedEndInput.classList.remove('input-error');
        return true;
    }

    // ---------- Submit — loading state ----------

    function onSubmit(e) {
        if (!validateDateRange()) {
            e.preventDefault();
            return;
        }

        if (submitBtn) submitBtn.disabled = true;
        if (btnText)   btnText.classList.add('hidden');
        if (btnLoading) btnLoading.classList.remove('hidden');

        // Sunucu yanıt verince (sayfa yenilenince) buton zaten sıfırlanır.
        // Hata durumunda (sunucu formu geri dönerse) sayfa reload olmaz,
        // bu yüzden kısa süre sonra tekrar aktif edelim.
        setTimeout(function () {
            if (submitBtn) submitBtn.disabled = false;
            if (btnText)   btnText.classList.remove('hidden');
            if (btnLoading) btnLoading.classList.add('hidden');
        }, 30000);
    }

    // ---------- Event listener'lar ----------

    // JQL preview: her değişiklikte güncelle
    const watchedInputs = [
        mudurlukInput,
        createdStartDateInput,
        statusChangedStartInput,
        statusChangedEndInput
    ];

    watchedInputs.forEach(function (el) {
        if (el) el.addEventListener('change', buildJqlPreview);
        if (el) el.addEventListener('input', buildJqlPreview);
    });

    // Müdürlük değiştiğinde JQL'i güncelle
    if (mudurlukInput) {
        mudurlukInput.addEventListener('change', buildJqlPreview);
    }

    document.querySelectorAll('input[name="issueTypes"], input[name="statuses"]')
        .forEach(function (cb) {
            cb.addEventListener('change', buildJqlPreview);
        });

    // Tarih aralığı kontrolü
    if (statusChangedEndInput) {
        statusChangedEndInput.addEventListener('change', validateDateRange);
    }
    if (statusChangedStartInput) {
        statusChangedStartInput.addEventListener('change', validateDateRange);
    }

    // Submit
    if (form) {
        form.addEventListener('submit', onSubmit);
    }

    // Sayfa yüklenince status changed tarihlerini bu ayın 1. ve son günüyle doldur
    (function setDefaultDateRange() {
        if (!statusChangedStartInput || !statusChangedEndInput) return;
        if (statusChangedStartInput.value && statusChangedEndInput.value) return; // sunucu zaten doldurmuşsa dokunma

        const now       = new Date();
        const year      = now.getFullYear();
        const month     = now.getMonth(); // 0-bazlı

        const firstDay  = new Date(year, month, 1);
        const lastDay   = new Date(year, month + 1, 0); // bir sonraki ayın 0. günü = bu ayın son günü

        function toIso(d) {
            const y  = d.getFullYear();
            const m  = String(d.getMonth() + 1).padStart(2, '0');
            const dd = String(d.getDate()).padStart(2, '0');
            return `${y}-${m}-${dd}`;
        }

        if (!statusChangedStartInput.value) statusChangedStartInput.value = toIso(firstDay);
        if (!statusChangedEndInput.value)   statusChangedEndInput.value   = toIso(lastDay);
    })();

    // Sayfa yüklenince preview'i güncelle
    buildJqlPreview();

})();
