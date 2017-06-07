import java.util.*;

public class TxHandler {

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	 * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	 * constructor.
	 */

	private UTXOPool pool;

	public TxHandler(UTXOPool utxoPool) {
		this.pool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if:
	 * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
	 * (2) the signatures on each input of {@code tx} are valid,
	 * (3) no UTXO is claimed multiple times by {@code tx},
	 * (4) all of {@code tx}s output values are non-negative, and
	 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
	 *     values; and false otherwise.
	 */
	// public UTXOPool() {
    //     H = new HashMap<UTXO, Transaction.Output>();
    // }
    // public UTXO(byte[] txHash, int index) {
    //     this.txHash = Arrays.copyOf(txHash, txHash.length);
    //     this.index = index;
    // }
    // public Output(double v, PublicKey addr) {
    //     value = v;
    //     address = addr;
    // }
	public boolean isValidTx(Transaction tx) {
		UTXOPool doubleCheck = new UTXOPool();
		double totalOut = 0;

		for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input input = tx.getInput(i);
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			Transaction.Output output = pool.getTxOutput(utxo);
			if (output == null) return false;
			if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature))
				return false;
			if (doubleCheck.contains(utxo)) return false;
			doubleCheck.addUTXO(utxo, output);
			totalOut += output.value;
		}
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for (Transaction.Output output : outputs) {
			if (output.value < 0) return false;
			totalOut -= output.value;
		}
		if (totalOut < 0) return false;
		return true;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions, checking each
	 * transaction for correctness, returning a mutually valid array of accepted transactions, and
	 * updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> txs = new ArrayList<Transaction>();
		for (Transaction tx : possibleTxs) {
			if (isValidTx(tx)) {
				txs.add(tx);
				ArrayList<Transaction.Input> inputs = tx.getInputs();
				for (Transaction.Input input : inputs) {
					pool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
				}
				for (int i = 0; i < tx.numOutputs(); i++) {
					pool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
				}
			}
		}
		return txs.toArray(new Transaction[txs.size()]);
	}

}
