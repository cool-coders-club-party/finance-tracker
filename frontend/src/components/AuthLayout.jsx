import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import Grainient from './Grainient';
import TextType from './TextType';

/**
 * The shared frame for the login and registration pages: animated background,
 * video panel, typed heading, and a card that holds whatever form is passed in.
 *
 * heading  the large typed greeting beside the card
 * title    the card's own title ("Sign in", "Create account")
 * children the form (and links) drawn inside the card
 */
export default function AuthLayout({ heading, title, children }) {
  return (
    <main className="login-page dark">
      <div aria-hidden="true" className="login-background">
        <Grainient
          colors={['#03041e', '#3b82f6', '#06b6d4', '#f1cbfc']}
          angle={100}
          bands={3}
          silk={0.6}
          flow={1.1}
          className="h-full w-full"
        />
        <div className="login-background-shade" />
      </div>
      <section className="login-card">
        <div className="login-media">
          <video className="login-video" autoPlay loop muted playsInline aria-hidden="true">
            <source src="/LoginSpinWithBackground.mp4" type="video/mp4" />
          </video>
        </div>
        <div className="login-content">
          {/* aria-label gives screen readers the full text, not half-typed letters. */}
          <h1 className="login-title" aria-label={heading}>
            <TextType
              as="span"
              text={heading}
              typingSpeed={65}
              initialDelay={250}
              loop={false}
              showCursor
              cursorStyle="underscore"
            />
          </h1>
          <Card className="glass mx-auto w-full max-w-md bg-card/40">
            <CardHeader>
              <CardTitle>{title}</CardTitle>
            </CardHeader>
            <CardContent>{children}</CardContent>
          </Card>
        </div>
      </section>
    </main>
  );
}