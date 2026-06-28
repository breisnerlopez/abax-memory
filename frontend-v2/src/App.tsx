import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import SearchPage from './pages/SearchPage';
import DetailPage from './pages/DetailPage';
import EditorPage from './pages/EditorPage';
import ReviewPage from './pages/ReviewPage';
import AdminPage from './pages/AdminPage';
import DashboardPage from './pages/DashboardPage';

const baseName = import.meta.env.BASE_URL.replace(/\/$/, '') || '/';

export default function App() {
  return (
    <BrowserRouter basename={baseName}>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<SearchPage />} />
          <Route path="detail/:id" element={<DetailPage />} />
          <Route path="create" element={<EditorPage />} />
          <Route path="edit/:id" element={<EditorPage />} />
          <Route path="review" element={<ReviewPage />} />
          <Route path="admin" element={<AdminPage />} />
          <Route path="dashboard" element={<DashboardPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
