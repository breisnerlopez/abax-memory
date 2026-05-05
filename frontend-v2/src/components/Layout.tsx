import { NavLink, Outlet } from 'react-router-dom';

// MOCK: Hardcoded tenant info // REPLACE_BEFORE_PROD
const MOCK_TENANT = { id: 'tenant-001', name: 'Default Tenant' };
const MOCK_USER = { name: 'Admin User', role: 'memory-admin' };

export default function Layout() {
  const navItems = [
    { to: '/', label: 'Search', icon: '🔍' },
    { to: '/create', label: 'Create', icon: '✏️' },
    { to: '/review', label: 'Review', icon: '✅' },
    { to: '/dashboard', label: 'Dashboard', icon: '📊' },
    { to: '/admin', label: 'Admin', icon: '⚙️' },
  ];

  return (
    <div className="app-layout">
      <header className="app-header">
        <div className="header-left">
          <h1 className="app-logo">
            <NavLink to="/">Abax-Memory</NavLink>
          </h1>
          <span className="header-version">v2.0.0</span>
        </div>

        <div className="header-center">
          <span className="tenant-badge" title={`Tenant: ${MOCK_TENANT.id}`}>
            🏢 {MOCK_TENANT.name}
          </span>
        </div>

        <div className="header-right">
          <span className="user-info">
            👤 {MOCK_USER.name}{' '}
            <span className="user-role">({MOCK_USER.role})</span>
          </span>
        </div>
      </header>

      <div className="app-body">
        <nav className="app-sidebar" aria-label="Main navigation">
          <ul>
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    isActive ? 'nav-link nav-link-active' : 'nav-link'
                  }
                >
                  <span className="nav-icon">{item.icon}</span>
                  <span className="nav-label">{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <main className="app-main">
          <Outlet />
        </main>
      </div>

      <footer className="app-footer">
        <span>Abax-Memory v2.0.0 — Frontend Multi-Dominio</span>
      </footer>
    </div>
  );
}
