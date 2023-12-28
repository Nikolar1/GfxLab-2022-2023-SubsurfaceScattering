package xyz.marsavic.gfxlab.graphics3d;


import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.random.sampling.Sampler;

public class GeometryUtils {

	//Helper class used for rotating a point around a line
	static class Quaternion {
		private double s,a,b,c;

		public Quaternion(double s, double a, double b, double c) {
			this.s = s;
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public Quaternion(Vec3 u){
			s = 0.0;
			a = u.x();
			b = u.y();
			c = u.z();
		}

		public Quaternion(double theta, Vec3 u){
			Vec3 v = u.normalized_();
			double k = Math.sin(theta/2.0);
			s = Math.cos(theta/2.0);
			a = v.x()*k;
			b = v.y()*k;
			c = v.z()*k;

		}

		public Quaternion conjugate(){
			return new Quaternion(s,-1*a,-1*b,-1*c);
		}

		public double length(){
			return Math.sqrt(s*s + a*a + b*b + c*c);
		}

		public double sqrLength(){
			return s*s + a*a + b*b + c*c;
		}

		public Quaternion multiply(double k){
			return new Quaternion(k*s,k*a,k*b,k*c);
		}

		public Quaternion multiply(Quaternion q){
			return new Quaternion(
					s*q.s - a*q.a - b*q.b - c*q.c,
					s*q.a + a*q.s + b*q.c - c*q.b,
					s*q.b + b*q.s + c*q.a - a*q.c,
					s*q.c + c*q.s + a*q.b - b*q.a);
		}

		public Quaternion invert(){
			double sqrLength = sqrLength();
			if(sqrLength == 0.0){
				return null;
			}
			return conjugate().multiply(1.0/(sqrLength));
		}
	}

	private static  final float TWO_PI = 6.28318530718f;
	
	/** An orthogonal vector to v. */
	public static Vec3 normal(Vec3 v) {
		if (v.x() != 0 || v.y() != 0) {
			return Vec3.xyz(-v.y(), v.x(), 0);
		} else {
			return Vec3.EX;
		}
	}
	
/*
	public static Vec3 normal(Vec3 v) {
		Vec3 p = v.cross(Vec3.EX);
		if (p.allZero()) {
			p = v.cross(Vec3.EY);
		}
		return p;
	}
*/

	public static Vec3 reflected(Vec3 n, Vec3 d) {
		return n.mul(2 * d.dot(n) / n.lengthSquared()).sub(d);
	}
	
	public static Vec3 reflectedN(Vec3 n_, Vec3 d) {
		return n_.mul(2 * d.dot(n_)).sub(d);
	}
	
	
	public static Vec3 refractedNN(Vec3 n_, Vec3 i_, double refractiveIndex) {
		double c1 = i_.dot(n_);
		double ri = c1 >= 0 ? refractiveIndex : -1.0 / refractiveIndex;
		double c2Sqr = 1 - (1 - c1 * c1) / (ri * ri);
		
		return c2Sqr > 0 ?
				n_.mul(c1 - Math.sqrt(c2Sqr) * ri).sub(i_) :    // refraction
				reflectedN(n_, i_);                             // total reflection
	}
	
	
	public static Vec3 sampleHemisphereCosineDistributedN(Sampler sampler, Vec3 n_) {
		// Sample the sphere with radius 1, add n_
		double x, y, z, lVSqr;

		// This could be done nicer using Vec3, but we like speed.
		do {
			x = 2 * sampler.uniform() - 1;
			y = 2 * sampler.uniform() - 1;
			z = 2 * sampler.uniform() - 1;
			lVSqr = x*x + y*y + z*z;
		} while (lVSqr > 1);

		double c = 1 / Math.sqrt(lVSqr);
		return Vec3.xyz(x * c, y * c, z * c).add(n_);
	}

	public static Vector gaussianSampleDisk(double u1, double u2, double falloff) {
		double r = Math.sqrt(Math.log(u1) / (-1*falloff));
		double theta = TWO_PI * u2;
		return Vector.polar(r, theta);
	}

	public static Vector gaussianSampleDisk(double u1, double u2, double falloff, double rMax) {
		double r = Math.sqrt(Math.log(1.0 - u1 * (1.0 - Math.exp(-1*falloff*rMax*rMax))) / (-1*falloff));
		double theta = TWO_PI * u2;
		return Vector.polar(r, theta);
	}

	public static Vec3 rotateAroundVector(Vec3 toRotate, Vec3 lBegin, Vec3 lEnd, double phi){
		Quaternion R = new Quaternion(phi, lEnd.sub(lBegin));
		Quaternion A = new Quaternion(phi, toRotate.sub(lBegin));
		Quaternion B = R.multiply(A).multiply(R.invert());
		return Vec3.xyz(B.a+lBegin.x(), B.b + lBegin.y(), B.c + lBegin.z());
	}

	//Mislim da ovaj  racun vec negde postoji ali ne mogu da se setim gde
	public static double distanceSquared(Vec3 a, Vec3 b){
		double xab2 = a.x() - b.x();
		double yab2 = a.y() - b.y();
		double zab2 = a.z() - b.z();
		return xab2 * xab2 + yab2 * yab2 + zab2 * zab2;
	}
	
}
