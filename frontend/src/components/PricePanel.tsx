import * as styles from "../styles";

interface Props {
    prices: Record<string, number>;
}

export default function PricePanel({ prices }: Props) {
    const entries = Object.entries(prices);
    return (
        <section style={styles.section}>
            <h2>현재가</h2>
            <div style={styles.card}>
                {entries.length === 0 ? (
                    <p style={styles.emptyText}>가격 정보가 없습니다.</p>
                ) : (
                    <div style={{ display: "flex", gap: 16, flexWrap: "wrap" }}>
                        {entries.map(([market, price]) => (
                            <div key={market}>
                                <strong>{market}</strong>: {price.toLocaleString("ko-KR")}원
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </section>
    );
}
