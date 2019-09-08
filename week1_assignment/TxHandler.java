import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TxHandler {

	private UTXOPool utxoPool;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
	 * by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are valid,
	 *         (3) no UTXO is claimed multiple times by {@code tx}, (4) all of
	 *         {@code tx}s output values are non-negative, and (5) the sum of
	 *         {@code tx}s input values is greater than or equal to the sum of its
	 *         output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		double total_out = 0;
		double total_in = 0;
		// * (1) all outputs claimed by {@code tx} are in the current UTXO pool,

		for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
			if (!utxoPool.contains(u)) {
				return false;
			}
		}

		// * (2) the signatures on each input of {@code tx} are valid,
		ArrayList<Transaction.Input> ins = tx.getInputs();

		for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
			Transaction.Output out = utxoPool.getTxOutput(utxo);
			boolean valid =  Crypto.verifySignature(out.address, tx.getRawDataToSign(i), in.signature);
			
			if (!valid) {
				return false;
			} else {
				total_in += out.value;
			}
		}

		// * (3) no UTXO is claimed multiple times by {@code tx},
		ArrayList<UTXO> allUTXO = utxoPool.getAllUTXO();
		Set<UTXO> setUTXO = new HashSet<UTXO>(allUTXO.size());
		for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
			if(!setUTXO.add(utxo)) {
				return false;
			}
		}
		
		//(4) all of {@code tx}s output values are non-negative, 
		
		for (int i = 0; i < tx.numOutputs(); i++) {
			Transaction.Output out = tx.getOutput(i);
			if(out.value < 0) {
				return false;
			} else {
				total_out += out.value;
			}
		}
		
		/*
		(5) the sum of
		 *         {@code tx}s input values is greater than or equal to the sum of its
		 *         output values; and false otherwise.
		 */
		
		 if (total_out > total_in) {
			 return false;
		 }
		 
		return true;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions,
	 * checking each transaction for correctness, returning a mutually valid array
	 * of accepted transactions, and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		List<Transaction> validTx = new ArrayList<Transaction>();		
		for ( int i =0 ; i < possibleTxs.length ; i++) {
			Transaction tx = possibleTxs[i];
			if (isValidTx(tx)) {
				validTx.add(tx);
				
				/* get all the input coins from the tx and remove them from Pool */
				ArrayList<Transaction.Input> ins = tx.getInputs();

				for (int j = 0; j < tx.numInputs(); j++) {
					Transaction.Input in = tx.getInput(j);
					Transaction.Output out = tx.getOutput(in.outputIndex);
					UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
					utxoPool.removeUTXO(u);
				}
				
				/* get all the output coins from the tx and add them from Pool */
				
				for (int k = 0; k < tx.numOutputs(); k++) {
					Transaction.Output out = tx.getOutput(k);
					UTXO u = new UTXO(tx.getHash(), k);
					utxoPool.addUTXO(u, out);
				}
				
			}
		}
		Transaction[] result = new Transaction[validTx.size()];
		validTx.toArray(result);
		return result;
	}

}
