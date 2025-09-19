import { useEffect, useState } from "react";

function App() {
    const [status, setStatus] = useState<string>("Loading...");

    useEffect(() => {
        fetch("http://localhost:8081/api/upbit/auto/status")
            .then((res) => res.text())
            .then((data) => setStatus(data))
            .catch((err) => setStatus("❌ 오류 발생: " + err));
    }, []);

    return (
        <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
            <h1>🚀 자동매매 상태 확인</h1>
            <p>{status}</p>
        </div>
    );
}

export default App;