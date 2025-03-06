import { Outlet, Link } from "react-router-dom";
import { useState } from "react";
import './navbar.css';
import external from '../../assets/external.png'
import logo from '../../assets/logo.png'
import '../../index.css'

const NavBar = () => {
  // active state
  const [isActive, setIsActive] = useState(false);

  // active class
  const toggleActiveClass = () => {
    setIsActive(!isActive);
  }

  // remove active class
  const removeActive = () => {
    setIsActive(false);
  }

  // github button
  const handleClick = () => {
    window.open("https://github.com/tkjonesy/Artificial-Intelligence-Monitoring-For-Surgery");
  }

  return (
    <>
      <nav className="nav-container">
        {/* logo */}
        <img className="logo" src={logo} alt="logo" />;

        <ul className="nav-list">
          <li className="nav-item">
            <Link className="link-special" to="/">Home</Link>
          </li>
          <li className="nav-item">
            <Link className="link-special" to="/software">Software</Link>
          </li>
          <li className="nav-item">
            <Link className="link-special" to="/model">AI Model</Link>
          </li>
          <li className="nav-item">
            <Link className="link-special" to="/other">Other Applications</Link>
          </li>
          <li className="nav-item">
            <Link className="link-special" to="/about">About</Link>
          </li>
        </ul>

        <button className="button" onClick={handleClick}>
          View on GitHub
          <img className="external" src={external} alt="external" />
        </button>
      </nav>

      <Outlet />
    </>
  );
};

export default NavBar;