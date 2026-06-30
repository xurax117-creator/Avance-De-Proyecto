(function () {
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

    const links = [
        { href: '/venta.html',             icon: '🛒', label: 'Punto de Venta',  key: 'venta'             },
        { href: '/inventario.html',         icon: '📦', label: 'Inventario',      key: 'inventario'        },
        { href: '/entrada-inventario.html', icon: '📥', label: 'Entradas',        key: 'entrada-inventario'},
        { href: '/reportes.html',           icon: '📊', label: 'Reportes',        key: 'reportes'          },
        { href: '/promociones.html',        icon: '🏷️', label: 'Promociones',     key: 'promociones'       },
        { href: '/proveedores.html',        icon: '🚚', label: 'Proveedores',     key: 'proveedores'       },
        { href: '/usuarios.html',           icon: '👥', label: 'Usuarios',        key: 'usuarios'          },
    ];

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
                <img src="/logo MRKdito.png" alt="MRKdito" class="sidebar-logo">
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
                <button class="nav-link" style="width:100%;text-align:left" onclick="mrk_logout()">
                    <span class="nav-icon">🚪</span>
                    <span>Cerrar Sesión</span>
                </button>
            </div>
        </nav>`;

    const layout = document.getElementById('layout');
    if (layout) layout.insertAdjacentHTML('afterbegin', html);

    window.mrk_logout = function () {
        if (confirm('¿Seguro que deseas cerrar sesión?')) {
            localStorage.clear();
            location.href = '/sucursal.html';
        }
    };
})();
