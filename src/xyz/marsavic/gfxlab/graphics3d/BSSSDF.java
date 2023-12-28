package xyz.marsavic.gfxlab.graphics3d;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.random.sampling.Sampler;

public class BSSSDF {

    public static final float INV_PI = 0.31830988618379067154f;
    private static final double SKIP_RATIO = 0.01;
    private static final Color ONE = Color.rgb(1,1,1);

    /*
    Donner. C 2006 Chapter 5
    sigmaA - absorb coefficient, describe how much radiance got absorbed in medium
    sigmaS - scatter coefficient, describe how much radiance got scattered in medium
    g - anisotropy parameter (range from -1 to 1, -1 for fully backward scattering, 1 for fully forward scattering, 0 for isotropy)
    eta - Relative refractive index
    sigmaT Extinction coefficient
    sigmaTr Effective extinction coefficient
    materialD Diffusion constant
    A albedo
    zr distance beneathe the surface to positive dipole light
    zv distance above the surface to negative dipole light
    dr distance from x to the positive dipole light
    dv distance from x to the negative dipole light
    alphaPrime reduced albedo
     */
    double fdr;
    double materialA;
    Color sigmaTPrime;
    Color sigmaTr;
    Color zr;
    Color materialD;
    Color zv;
    Color alphaPrime;
    double rMax;
    Color sigmaSPrime;
    Color sigmaA;
    Color sigmaS;
    double eta;
    double g;
    boolean absorbtive = false;

    public double getEta(){
        return eta;
    }


    public record Result(
            Vec3 outA,
            Vec3 outB
    ) {
        public static final BSSSDF.Result ABSORBED = new BSSSDF.Result(Vec3.ZERO, Vec3.ZERO);
    }

    public static BSSSDF ABSORPTIVE = new BSSSDF();
    //Data from A Practical Model for Subsurface Light Transport, Jensen
    public static BSSSDF APPLE = new BSSSDF(Color.rgb(0.003,0.0034,0.046), Color.rgb(2.29,2.39,1.97), 1.3);
    public static BSSSDF CHICKEN1 = new BSSSDF(Color.rgb(0.015, 0.077, 0.19), Color.rgb(0.15,0.21,0.38), 1.3);
    public static BSSSDF CHICKEN2 = new BSSSDF(Color.rgb(0.018,0.088,0.2), Color.rgb(0.19,0.25,0.32), 1.3);
    public static BSSSDF CREAM = new BSSSDF(Color.rgb(0.0002,0.0028,0.0163), Color.rgb(7.38,5.47,3.15), 1.3);
    public static BSSSDF KETCHUP = new BSSSDF(Color.rgb(0.061,0.97,1.45), Color.rgb(0.18,0.07,0.03), 1.3);
    public static BSSSDF MARBLE = new BSSSDF(Color.rgb(0.0021,0.0041,0.0071), Color.rgb(2.19,2.62,3), 1.5);
    public static BSSSDF POTATO = new BSSSDF(Color.rgb(0.0024,0.009,0.12), Color.rgb(0.68,0.7,0.55), 1.3);
    public static BSSSDF SKIMMILK = new BSSSDF(Color.rgb(0.0014,0.0025,0.0142), Color.rgb(0.7,1.22,1.9), 1.3);
    public static BSSSDF SKIN1 = new BSSSDF(Color.rgb(0.032,0.17,0.48), Color.rgb(0.74,0.88,1.01), 1.3);
    public static BSSSDF SKIN2 = new BSSSDF(Color.rgb(0.013,0.07,0.145), Color.rgb(1.09,1.59,1.79), 1.3);
    public static BSSSDF SPECTRALON = new BSSSDF(Color.rgb(0,0,0), Color.rgb(11.6,20.4,14.9), 1.3);
    public static BSSSDF WHOLEMILK = new BSSSDF(Color.rgb(0.011,0.0024,0.014), Color.rgb(2.55,3.21,3.77), 1.3);


    //We need any vector that is perpendicular to our normal so we can get the point in the disk needed for the ray
    //This might not be correct
    public BSSSDF.Result sample(Sampler sampler, Vec3 n_, Vec3 p, Vec3 i){
        if (absorbtive){
            return Result.ABSORBED;
        }
        //Vector pSample = GeometryUtils.gaussianSampleDisk(sampler.gaussian(), sampler.gaussian() , sigmaTr.luminance(), rMax);
        double r = rMax * Math.sqrt(sampler.gaussian());
        double phi = sampler.gaussian();
        //Very likely not right the rotate point around vector was taken from a book "Analiticka geometrija za informaticare" by Dragan Masulovic 2019
        //But I might be using it wrong
        Vec3 outA = GeometryUtils.rotateAroundVector(n_.rejection(i), p, n_, phi).normalizedTo(r);
        //by adding the coordinates of the point rotated around the normal we should get a point that is straight "below outA" and the ray outAoutB should be parallel to ray pd
        Vec3 outB = p.add(outA);
        return new Result(outA, outB);
    }

    public Color rd(double d2){
        Color dr = zr.mul(zr).add(d2).sqrt();
        Color dv = zv.mul(zv).add(d2).sqrt();
        Color sTrDr = sigmaTr.mul(dr);
        Color sTrDv = sigmaTr.mul(dv);
        return alphaPrime.mul(0.25 * INV_PI)
                .mul(
                        (zr
                                .mul(ONE.add(sTrDr))
                                .mul(sTrDr.negate().exp().div(dr.mul(dr).mul(dr)))
                                .add((zr
                                        .mul(ONE.add(sTrDv))
                                        .mul(sTrDv.negate().exp().div(dv.mul(dv).mul(dv)))))
                        ));
    }

    //Fdr - average diffuse Fresnel reflectance (according to Donner 06)
    static double fdr(double eta){
        if (eta < 1.0){
            return -0.4399 + 0.7099 / eta - 0.3319 / (eta * eta) + 0.0636 / (eta * eta * eta);
        }else {
            return -1.4399 / (eta * eta) + 0.7099 / eta + 0.6681 + 0.0636 * eta;
        }
    }


    public BSSSDF(){
        absorbtive = true;
    }
    public BSSSDF(Color sigmaA, Color sigmaS, double eta, double g) {
        this.g = g;
        this.sigmaS = sigmaS;
        Color sigmaSPrime = sigmaS.mul(1-g);
        new BSSSDF(sigmaA, sigmaSPrime, eta);
    }
    public BSSSDF(Color sigmaA, Color sigmaSPrime, double eta) {
        this.sigmaA = sigmaA;
        this.sigmaSPrime = sigmaSPrime;
        this.eta = eta;
        fdr = fdr(eta);
        materialA = (1.0 + fdr) / (1.0 - fdr);
        sigmaTPrime = sigmaA.add(sigmaSPrime);
        sigmaTr = sigmaA.mul(3.0).mul(sigmaTPrime).sqrt();
        zr = ONE.div(sigmaTPrime);
        materialD = ONE.div(sigmaTPrime.mul(3));
        zv = zr.add(materialD.mul(4).mul(materialA));
        alphaPrime = sigmaSPrime.div(sigmaTPrime);
        rMax = Math.sqrt(Math.log(SKIP_RATIO) / (-1 * sigmaTr.luminance()));
    }

}
