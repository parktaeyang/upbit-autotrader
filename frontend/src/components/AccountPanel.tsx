import * as styles from "../styles";
import type { Account } from "../api/types";

interface Props {
    accounts: Account[] | null;
}

export default function AccountPanel({ accounts }: Props) {
    return (
        <section style={styles.section}>
            <h2>계정 정보</h2>
            <div style={styles.card}>
                {accounts ? (
                    <pre style={styles.pre}>{JSON.stringify(accounts, null, 2)}</pre>
                ) : (
                    <p style={styles.emptyText}>조회된 정보가 없습니다.</p>
                )}
            </div>
        </section>
    );
}
