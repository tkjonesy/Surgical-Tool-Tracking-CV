import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter, Routes, Route } from 'react-router-dom';
import Home from "./pages/home/home";
import About from "./pages/about/about";
import NavBar from './components/navbar/navbar';
import './App.css';

function App() {

  return (
    <HashRouter>
      <NavBar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="about" element={<About />} />
      </Routes>
    </HashRouter>
  );
}

export default App;