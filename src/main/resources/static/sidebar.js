(function () {
    // Prevent browser 404 on favicon
    if (!document.querySelector('link[rel~="icon"]')) {
        const fav = document.createElement('link');
        fav.rel = 'icon';
        fav.href = "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🛒</text></svg>";
        document.head.appendChild(fav);
    }

    // Apply saved theme immediately to prevent flash
    const savedTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);

    const userId = localStorage.getItem('userId');
    const idSucursal = localStorage.getItem('idSucursal');
    if (!userId || !idSucursal) {
        window.location.href = '/sucursal.html';
        return;
    }

    const nombre     = localStorage.getItem('nombreUsuario') || '';
    const rol        = localStorage.getItem('rolUsuario') || '';
    const nombreSuc  = localStorage.getItem('nombreSucursal') || 'Sucursal';
    const color      = localStorage.getItem('colorSucursal') || '#19A337';

    document.documentElement.style.setProperty('--primary', color);

    const page = location.pathname.split('/').pop().replace('.html', '');

    const PERMISOS = {
        'venta':              ['Administrador', 'Gerente', 'Cajero'],
        'inventario':         ['Administrador', 'Gerente'],
        'entrada-inventario': ['Administrador', 'Gerente', 'Cajero'],
        'reportes':           ['Administrador', 'Gerente'],
        'promociones':        ['Administrador', 'Gerente'],
        'proveedores':        ['Administrador', 'Gerente'],
        'usuarios':           ['Administrador'],
    };

    // Guard: si el rol no tiene permiso en esta página, redirigir
    const rolesPermitidos = PERMISOS[page];
    if (rolesPermitidos && !rolesPermitidos.includes(rol)) {
        window.location.href = '/venta.html';
        return;
    }

    const links = [
        { href: '/venta.html',             icon: '🛒', label: 'Punto de Venta',  key: 'venta'             },
        { href: '/inventario.html',         icon: '📦', label: 'Inventario',      key: 'inventario'        },
        { href: '/entrada-inventario.html', icon: '📥', label: 'Entradas',        key: 'entrada-inventario'},
        { href: '/reportes.html',           icon: '📊', label: 'Reportes',        key: 'reportes'          },
        { href: '/promociones.html',        icon: '🏷️', label: 'Promociones',     key: 'promociones'       },
        { href: '/proveedores.html',        icon: '🚚', label: 'Proveedores',     key: 'proveedores'       },
        { href: '/usuarios.html',           icon: '👥', label: 'Usuarios',        key: 'usuarios'          },
    ].filter(l => (PERMISOS[l.key] || []).includes(rol));

    const navHtml = links.map(l => `
        <li>
            <a href="${l.href}" class="nav-link${page === l.key ? ' active' : ''}">
                <span class="nav-icon">${l.icon}</span>
                <span>${l.label}</span>
            </a>
        </li>`).join('');

    const initials = nombre.trim().split(/\s+/).slice(0, 2).map(n => n[0] || '').join('').toUpperCase() || '?';

    const html = `
        <nav class="sidebar">
            <div class="sidebar-brand">
                <img src="/mrkdito logo sin fondo real.png" alt="MRKdito" class="sidebar-logo">
                <div class="sidebar-branch">
                    <span>📍</span>
                    <span>${nombreSuc}</span>
                </div>
            </div>
            <div class="sidebar-user">
                <div class="user-avatar">${initials}</div>
                <div class="user-details">
                    <div class="user-name">${nombre}</div>
                    <div class="user-role">${rol}</div>
                </div>
            </div>
            <ul class="sidebar-nav">${navHtml}</ul>
            <div class="sidebar-footer">
                <button class="nav-link" id="mrk-theme-btn" style="width:100%;text-align:left" onclick="mrk_toggleTheme()">
                    <span class="nav-icon" id="mrk-theme-icon">🌙</span>
                    <span id="mrk-theme-label">Modo Claro</span>
                </button>
                <button class="nav-link" style="width:100%;text-align:left" onclick="mrk_logout()">
                    <span class="nav-icon">🚪</span>
                    <span>Cerrar Sesión</span>
                </button>
            </div>
        </nav>`;

    function updateThemeBtn() {
        const current = document.documentElement.getAttribute('data-theme') || 'dark';
        const icon  = document.getElementById('mrk-theme-icon');
        const label = document.getElementById('mrk-theme-label');
        if (!icon || !label) return;
        if (current === 'dark') {
            icon.textContent  = '☀️';
            label.textContent = 'Modo Claro';
        } else {
            icon.textContent  = '🌙';
            label.textContent = 'Modo Oscuro';
        }
    }

    const layout = document.getElementById('layout');
    if (layout) layout.insertAdjacentHTML('afterbegin', html);

    updateThemeBtn();

    window.mrk_logout = function () {
        if (confirm('¿Seguro que deseas cerrar sesión?')) {
            const tema = localStorage.getItem('theme');
            localStorage.clear();
            if (tema) localStorage.setItem('theme', tema);
            location.href = '/sucursal.html';
        }
    };

    window.mrk_toggleTheme = function () {
        const current = document.documentElement.getAttribute('data-theme') || 'dark';
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
        updateThemeBtn();
    };

    window.mrk_updateThemeButton = updateThemeBtn;
})();
