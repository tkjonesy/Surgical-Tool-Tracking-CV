import React, { useState } from "react";
import {Link} from "react-router-dom";
import "../../index.css";

function Home() {
    return (
        <>
            <div className="title-container">
            <div className="title">
                AIM(s): Artificial Intelligence Monitoring for Surgery
            </div>

            <div className="subheading">
                A full system solution to track surgical instrument usage in the operating room.
            </div>
            </div>
            
            <div class="wave">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 20 1440 120"><path fill="#000000" fill-opacity="1" d="M0,32L60,32C120,32,240,32,360,48C480,64,600,96,720,96C840,96,960,64,1080,48C1200,32,1320,32,1380,32L1440,32L1440,0L1380,0C1320,0,1200,0,1080,0C960,0,840,0,720,0C600,0,480,0,360,0C240,0,120,0,60,0L0,0Z"></path></svg>
            </div>
        </>
    )
}

export default Home;