import * as styles from "../styles";
import type { Account } from "../api/types";

interface Props {
    accounts: Account[] | null;
    prices: Record<string, number>;
    tradingMarkets: string[];
}

function formatKrw(amount: number): string {
    return `${amount.toLocaleString("ko-KR", { maximumFractionDigits: 0 })}원`;
}

export default function AccountPanel({ accounts, prices, tradingMarkets }: Props) {
    if (!accounts) {
        return (
            <section style={styles.section}>
                <h2>계정 정보</h2>
                <div style={styles.card}>
                    <p style={styles.emptyText}>조회된 정보가 없습니다.</p>
                </div>
            </section>
        );
    }

    const krwAccount = accounts.find((acc) => acc.currency === "KRW");
    const krwBalance = krwAccount ? Number(krwAccount.balance) : 0;
    const coinAccounts = accounts.filter(
        (acc) =>
            acc.currency !== "KRW" &&
            Number(acc.balance) > 0 &&
            tradingMarkets.includes(`KRW-${acc.currency}`)
    );

    return (
        <section style={styles.section}>
            <h2>계정 정보</h2>
            <div style={styles.card}>
                <div>
                    <strong>보유 원화</strong>: {formatKrw(krwBalance)}
                </div>

                <hr style={styles.divider} />

                <strong>보유 코인</strong>
                {coinAccounts.length === 0 ? (
                    <p style={styles.emptyText}>보유 중인 코인이 없습니다.</p>
                ) : (
                    <table style={styles.table}>
                        <thead>
                            <tr>
                                <th style={styles.th}>코인</th>
                                <th style={styles.th}>구매금액</th>
                                <th style={styles.th}>현재금액</th>
                            </tr>
                        </thead>
                        <tbody>
                            {coinAccounts.map((acc) => {
                                const balance = Number(acc.balance);
                                const avgBuyPrice = Number(acc.avg_buy_price);
                                const purchaseAmount = balance * avgBuyPrice;
                                const currentPrice = prices[`KRW-${acc.currency}`];
                                const currentAmount = currentPrice !== undefined ? balance * currentPrice : null;
                                return (
                                    <tr key={acc.currency}>
                                        <td style={styles.td}>{acc.currency}</td>
                                        <td style={styles.td}>{formatKrw(purchaseAmount)}</td>
                                        <td style={styles.td}>
                                            {currentAmount !== null ? formatKrw(currentAmount) : "-"}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}
            </div>
        </section>
    );
}
