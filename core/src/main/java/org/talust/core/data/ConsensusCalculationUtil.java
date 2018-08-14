package org.talust.core.data;

import org.talust.common.tools.Constant;
import org.talust.common.model.Coin;

/**
 * 共识计算工具
 */
public final class ConsensusCalculationUtil {

	/**
	 * 奖励总数
	 */
	public final static Coin TOTAL_REWARD = Coin.MAX;
	/**
	 * 初始共识奖励系数
	 */
	public final static Coin START_REWARD = Coin.COIN.multiply(256);
	/**
	 * 初始矿机奖励系数
	 */
	public final static Coin START_MINER = Coin.COIN.multiply(16);
	/**
	 * 最小共识奖励系数，从开始，每年两年减半，至少达到最小奖励，后面不变
	 */
	public final static Coin MIN_REWARD = START_REWARD.div(8);

	/**
	 * 最小矿机奖励系数，从开始，每年两年减半，至少达到最小奖励，后面不变
	 */
	public final static Coin MIN_MINER = START_MINER.div(8);
	/**
	 * 开始发放奖励的区块高度
	 */
	public final static long START_HEIGHT = Constant.START_HEIGHT;
	/**
	 * 每多少个区块奖励减半，也就是奖励周期，这里设置2年的出块数
	 */
	public final static long REWARD_CYCLE = 10512000L;
	
	/**
	 * 计算共识奖励
	 * 根据传入的区块高度，计算当前的奖励系数
	 * @param height
	 * @return Coin
	 */
	public final static Coin calculatConsensusReward(long height) {
		if(height>33112800L){
			return Coin.ZERO;
		}
		if(height < START_HEIGHT) {
			return Coin.ZERO;
		}
		//奖励周期
		long realHeight = (height - START_HEIGHT);
		long coefficient = realHeight / REWARD_CYCLE;
		//奖励系数
		Coin coefficientReward = START_REWARD;
		Coin issued = Coin.ZERO;
		long cycle = realHeight % REWARD_CYCLE;
		while(cycle-- > 0) {
			if(coefficientReward.isGreaterThan(MIN_REWARD)){
				coefficientReward = coefficientReward.divide(2);
			}else{
				coefficientReward = MIN_REWARD;
			}
		}
		issued = issued.add(coefficientReward);
		//余量
		Coin balance = TOTAL_REWARD.subtract(issued);
		if(balance.isLessThan(Coin.ZERO)) {
			coefficientReward = Coin.ZERO;
		} else if(balance.isGreaterThan(Coin.ZERO) && balance.isLessThan(MIN_REWARD)) {
			coefficientReward = balance;
		}
		return coefficientReward;
	}

	/**
	 * 计算挖矿矿机奖励
	 * 根据传入的区块高度，计算当前的矿机奖励
	 */
	public final static Coin calculatMinerReward(long height){
		if(height>33112800L){
			return Coin.ZERO;
		}
		if(height < START_HEIGHT) {
			return Coin.ZERO;
		}
		long realHeight = (height - START_HEIGHT);
		Coin coefficientReward = START_MINER;
		Coin issued = Coin.ZERO;
		long cycle = realHeight % REWARD_CYCLE;
		while(cycle-- > 0) {
			if(coefficientReward.isGreaterThan(MIN_MINER)){
				coefficientReward = coefficientReward.divide(2);
			}else{
				coefficientReward = MIN_MINER;
			}
		}
		issued = issued.add(coefficientReward);
		Coin balance = TOTAL_REWARD.subtract(issued);
		if(balance.isLessThan(Coin.ZERO)) {
			coefficientReward = Coin.ZERO;
		} else if(balance.isGreaterThan(Coin.ZERO) && balance.isLessThan(MIN_MINER)) {
			coefficientReward = balance;
		}
		return coefficientReward;
	}



	/**
	 * 计算共识奖励
	 * 根据传入的区块高度，计算当前已产出的奖励总量
	 * 
	 * @param height
	 * @return Coin
	 */
	public final static Coin calculatTotal(long height) {
		if(height < START_HEIGHT) {
			return Coin.ZERO;
		}
		//奖励周期
		long realHeight = (height - START_HEIGHT);
		long coefficient = realHeight / REWARD_CYCLE;
		//奖励系数
		Coin coefficientReward = START_REWARD;
		Coin issued = Coin.ZERO;
		while(coefficient-- > 0) {
			issued = issued.add(coefficientReward.multiply(REWARD_CYCLE));
			coefficientReward = coefficientReward.div(2);
			if(coefficientReward.isLessThan(MIN_REWARD)) {
				coefficientReward = MIN_REWARD;
			}
		}
		return issued.add(coefficientReward.multiply(realHeight % REWARD_CYCLE));
	}

	/**
	 * 计算当前共识所需保证金
	 * @param currentConsensusSize
	 * @return Coin
	 */
	public static Coin calculatRecognizance(int currentConsensusSize, long height) {
		
		//max is (Math.log((double)300)/Math.log((double)2))
		
		double max = 2468d;

		double lgN = Math.log((double)currentConsensusSize)/Math.log((double)2);
		double nlgn = lgN*currentConsensusSize;
		long res = (long) (Coin.COIN.multiply(10000).value * (nlgn/max));

		if(res > Coin.COIN.multiply(1000000).value) {
			return Coin.COIN.multiply(1000000);
		} else if(res <Coin.COIN.multiply(10000).value) {
			return  Coin.COIN.multiply(10000);
		} else {
			if(res > Coin.COIN_VALUE * 100000) {
				return Coin.COIN.multiply(res/(Coin.COIN_VALUE * 10000)).multiply(10000);
			} else {
				return Coin.COIN.multiply(res/(Coin.COIN_VALUE * 1000)).multiply(1000);
			}
		}
	}
	
	public static long getConsensusCredit(long height) {
		return 1;
	}
	
	public static void main(String[] args) {
//		System.out.println(calculatTotal(REWARD_CYCLE * 16 + 3026900));
		
		int size = 308;
		for (int i = 1; i <= size; i ++) {
			System.out.println(i+"   "+calculatRecognizance(i, 5000000));
		}
	}
}
