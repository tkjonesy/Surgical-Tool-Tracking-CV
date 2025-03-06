import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter, Routes, Route } from 'react-router-dom';
import Home from "./pages/home/home";
import About from "./pages/about/about";
import NavBar from './components/navbar/navbar';
import Software from './pages/software/software';
import Model from './pages/model/model';
import Other from './pages/other/other';
import './App.css';
import './index.css';

function App() {

  return (
    <HashRouter>
      <NavBar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="software" element={<Software />} />
        <Route path="model" element={<Model />} />
        <Route path="other" element={<Other />} />
        <Route path="about" element={<About />} />
      </Routes>
    </HashRouter>
  );
}

export default App;