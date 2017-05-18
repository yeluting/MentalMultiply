package wyf.MentalMultiply;

public class Complex {
double real, imagine;

Complex(double real, double imagine) {
   this.real = real;
   this.imagine = imagine;
}

Complex mul(Complex x) {
   double r = real * x.real - imagine * x.imagine;
   double i = real * x.imagine + imagine * x.real;
   return new Complex(r, i);
}

Complex mul(double x) {
   return new Complex(x * real, x * imagine);
}

void muleq(double x) {
   real *= x;
   imagine *= x;
}

Complex add(Complex x) {
   return new Complex(real + x.real, imagine + x.imagine);
}

Complex sub(Complex x) {
   return new Complex(real - x.real, imagine - x.imagine);
}

void addeq(Complex x) {
   real += x.real;
   imagine += x.imagine;
}

Complex div(double x) {
   return new Complex(real / x, imagine / x);
}

void diveq(double x) {
   real /= x;
   imagine /= x;
}

double phi() {
   return Math.atan2(imagine, real);
}

double magnitude() {
   return Math.hypot(real, imagine);
}

@Override
public String toString() {
   return String.format("(%.2f,%.2fj)", real, imagine);
}

}
