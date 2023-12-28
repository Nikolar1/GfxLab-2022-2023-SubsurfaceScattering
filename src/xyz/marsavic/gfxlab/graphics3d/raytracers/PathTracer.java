package xyz.marsavic.gfxlab.graphics3d.raytracers;

import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.*;
import xyz.marsavic.random.sampling.Sampler;
import xyz.marsavic.utils.Hashing;


public class PathTracer extends RayTracer {
	
	private static final double EPSILON = 1e-9;
	private static final long seed = 0x68EFD508E309A865L;
	
	private final int maxDepth;
	
	
	public PathTracer(Scene scene, Camera camera, int maxDepth) {
		super(scene, camera);
		this.maxDepth = maxDepth;
	}
	
	
	@Override
	protected Color sample(Ray ray) {
		return radiance(ray, maxDepth, new Sampler(Hashing.mix(seed, ray)));
	}
	
	
	private Color radiance(Ray ray, int depthRemaining, Sampler sampler) {
		if (depthRemaining <= 0) return Color.BLACK;

		Hit hit = scene.solid().firstHit(ray, EPSILON);
		if (hit.t() == Double.POSITIVE_INFINITY) {
			return scene.colorBackground();
		}
		return radiance(ray, depthRemaining - 1, sampler, hit);
	}

	private Color radiance(Ray ray, int depthRemaining, Sampler sampler, Hit hit) {
		if (depthRemaining <= 0) return Color.BLACK;

		Material material = hit.material();
		Color result = material.emittance();

		Vec3 i = ray.d().inverse();                 // Incoming direction
		Vec3 n_ = hit.n_();                         // Normalized normal to the body surface at the hit point
		Vec3 p = ray.at(hit.t());               // Point of collision
		BSDF.Result bsdfResult = material.bsdf().sample(sampler, n_, i);
		if (bsdfResult.color().notZero()) {
			Ray rayScattered = Ray.pd(p, bsdfResult.out());
			Color rO = radiance(rayScattered, depthRemaining - 1, sampler);
			Color rI = rO.mul(bsdfResult.color());
			result = result.add(rI);
		}
		BSSSDF.Result bssdfResult = material.bsssdf().sample(sampler, n_, p, i);
		if ( bssdfResult.outA() != Vec3.ZERO) {
			double ft = 1.0 - fresnelDielectric(Math.abs(i.dot(n_)), 1.0, material.bsssdf().getEta());
			Ray rayScattered = Ray.pd(bssdfResult.outA(), bssdfResult.outB());
			Hit hit2 = scene.solid().firstHit(rayScattered, EPSILON);
			// == is intentional im checking if I hit the exact same object again
			if (hit2.t() != Double.POSITIVE_INFINITY && hit2.material().bsssdf() == material.bsssdf()) {
				Vec3 i2 = rayScattered.d().inverse();
				Vec3 n2_ = hit2.n_();
				double d = GeometryUtils.distanceSquared(p, rayScattered.at(hit2.t()));
				double fti = 1.0 - fresnelDielectric(Math.abs(i2.dot(n2_)), 1.0, material.bsssdf().getEta());
				Color rO = radiance(rayScattered, depthRemaining - 1, sampler, hit2);
				Color ssscatered = material.bsssdf().rd(d).mul(ft).mul(fti).mul(BSSSDF.INV_PI);
				Color rI = rO.mul(ssscatered);
				result = result.add(rI);
			}
		}
		return result;
	}

	private double fresnelDielectric(double cosi, double etai, double etat) {
		cosi = Math.max(cosi, Math.min(-1.0, 1.0));
		double sint = (etai / etat) * (double) Math.sqrt(Math.max(0.0, 1.0 - cosi * cosi));

		// total reflection
		if (sint >= 1.0) {
			return 1.0;
		}

		double cost = Math.sqrt(Math.max(0.0, 1 - sint * sint));
		cosi = Math.abs(cosi);

		double rParl = ((etat * cosi) - (etai * cost)) / ((etat * cosi) + (etai * cost));
		double rPerp = ((etai * cosi) - (etat * cost)) / ((etai * cosi) + (etat * cost));

		return (rParl * rParl + rPerp * rPerp) / 2.0;
	}

}
