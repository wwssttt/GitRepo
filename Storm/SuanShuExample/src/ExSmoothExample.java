public class ExSmoothExample {
	
	private static double exponentSmoothingSingle(double[] series){
		int arrSize = series.length;
        double s[] = new double[arrSize];
        double sse = 0, mse = 2E16, alpha = 0;
        s[0] = 0;
        // ac - different values of alpha
        for(double ac=0;ac<=1;ac+=0.1)
        {
            s[1] = series[0];
            sse = 0;
            double temp = 0;
            for(int i =2;i<arrSize;i++)
            {
                s[i] = (ac * series[i-1]) + ((1-ac)*s[i-1]);
                sse += Math.pow(s[i] - series[i],2);
            }
            temp = sse/ arrSize;
            if(temp < mse)
            {
                mse = temp;
                alpha = ac;
            }
        }
        double next = alpha * series[arrSize-1] + (1 - alpha) * s[arrSize - 1];
        return next;
	}
	
	public static void main(String[] args) {
        double[] y = { -36.72662900};
        System.out.println("next = "+exponentSmoothingSingle(y));
    }
}